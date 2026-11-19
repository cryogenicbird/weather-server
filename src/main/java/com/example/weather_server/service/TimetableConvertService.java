package com.example.weather_server.service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import com.example.weather_server.WeatherApiException;

/** 课表文件转换：POI 抽出单元格文本 → LLM 转成 App 能解析的格式 */
@Service
public class TimetableConvertService {

    private static final String[] DAYS = {
        "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"
    };

    private final RestClient client = RestClient.builder().build();

    // LLM 配置：url/model 写在 application.properties；key 从环境变量 LLM_API_KEY 注入（不入库）
    @Value("${llm.api.url}")
    private String llmUrl;

    @Value("${llm.model}")
    private String llmModel;
    

    @Value("${llm.api.key:}")
    private String llmKey;

    public String convert(MultipartFile file) {
        String raw = extractCells(file);
        if (raw.isBlank()) {
            throw new IllegalArgumentException("文件里没有读到课表内容");
        }
        return callLlm(raw);
    }

    /** 把 xlsx 里每个有内容的单元格转成一行 "星期X | 内容" */
    String extractCells(MultipartFile file) {
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            // 第 1 步：找表头行（出现"星期一"的那一行），记下"列号 → 星期名"
            int headerRow = -1;
            Map<Integer, String> colToDay = new HashMap<>();
            for (Row row : sheet) {
                boolean isHeader = false;
                for (Cell cell : row) {
                    if (fmt.formatCellValue(cell).trim().equals("星期一")) {
                        isHeader = true;
                        break;
                    }
                }
                if (!isHeader) {
                    continue;
                }
                headerRow = row.getRowNum();
                for (Cell cell : row) {
                    String v = fmt.formatCellValue(cell).trim();
                    for (String day : DAYS) {
                        if (day.equals(v)) {
                            colToDay.put(cell.getColumnIndex(), day);
                        }
                    }
                }
                break;
            }
            if (headerRow < 0) {
                throw new IllegalArgumentException("没找到表头（星期一~星期日）这个表格能识别吗？");
            }

            // 第 2 步：表头以下每个有内容的单元格 → 一行文本
            // 用 Set 去重：部分课表模板会把周六/周日区块重复写两遍，内容完全相同即为重复
            Set<String> lines = new LinkedHashSet<>();
            for (Row row : sheet) {
                if (row.getRowNum() <= headerRow) {
                    continue;
                }
                for (Cell cell : row) {
                    String day = colToDay.get(cell.getColumnIndex());
                    if (day == null) {
                        continue;
                    }
                    String v = fmt.formatCellValue(cell).trim();
                    if (v.isEmpty()) {
                        continue;
                    }
                    // 单元格内换行（多门课）压成 " / "，方便 LLM 分词
                    lines.add(day + " | " + v.replace("\r", " ").replace("\n", " / "));
                }
            }
            return String.join("\n", lines);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析这个 Excel 文件", e);
        }
    }

    /** 调 LLM：把原始单元格文本转成 App 的课表格式 */
    private String callLlm(String raw) {
        if (llmKey == null || llmKey.isBlank()) {
            throw new IllegalStateException("未配置 LLM_API_KEY 环境变量");
        }
        String systemPrompt = """
            你是课表格式转换器。把输入转换成如下格式，每行一门课，字段用空格分隔：
            周一 课程名 老师 1-2节 教室 3-7周
            规则：
            1. 周次含多段（如 [1-4,6-9周]）时拆成多行：1-4周一行、6-9周一行；
            2. 没有老师信息时，老师字段输出 - ；
            3. 一个单元格含多门课时，每门课单独成行；
            4. 只输出转换后的文本行，不要解释、不要 markdown 代码块。
            示例输入：
            星期一 | 数据结构 [1-8周] [1-2] D101
            示例输出：
            周一 数据结构 - 1-2节 D101 1-8周
            """;

        Map<String, Object> body = Map.of(
            "model", llmModel,
            "temperature", 0.2,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", raw)));

        try {
            Map<String, Object> resp = client.post()
                .uri(llmUrl)
                .header("Authorization", "Bearer " + llmKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

            List<Map<String, Object>> choices =
                (List<Map<String, Object>>) resp.get("choices");
            Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");
            return ((String) message.get("content")).trim();
        } catch (RestClientException e) {
            // 语义和天气代理一样：上游服务失败 → 502
            throw new WeatherApiException("AI 转换服务调用失败", e);
        }
    }
}
