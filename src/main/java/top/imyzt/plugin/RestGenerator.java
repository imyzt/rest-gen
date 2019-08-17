package top.imyzt.plugin;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.imyzt.plugin.utils.RestUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RestGenerator extends AnAction {

    private static final Logger log = LoggerFactory.getLogger(RestGenerator.class);

    /**
     * 方法模板
     */
    private static final String METHOD_TEMPLATE = "${requestMethod}(\"${path}${pathArgs}\")\n" +
            "\tpublic void ${methodName}(${args}) {\n" +
            "\t\t\n" +
            "\t}";

    /**
     * 请求方法映射
     */
    private static final Map<String, String> REQUEST_METHOD_MAP = new HashMap<>(4);
    static {
        REQUEST_METHOD_MAP.put("get", "@GetMapping");
        REQUEST_METHOD_MAP.put("post", "@PostMapping");
        REQUEST_METHOD_MAP.put("put", "@PutMapping");
        REQUEST_METHOD_MAP.put("delete", "@DeleteMapping");
    }

    /**
     * 字符串转换java常规类型
     */
    private static LinkedHashMap<String, String> JAVA_TYPE_MAP = new LinkedHashMap<>(9);
    static {
        JAVA_TYPE_MAP.put("string", "String");
        JAVA_TYPE_MAP.put("int", "Integer");
        JAVA_TYPE_MAP.put("float", "Float");
        JAVA_TYPE_MAP.put("long", "Long");
        JAVA_TYPE_MAP.put("double", "Double");
        JAVA_TYPE_MAP.put("byte", "Byte");
        JAVA_TYPE_MAP.put("char", "Character");
        JAVA_TYPE_MAP.put("boolean", "Boolean");
        JAVA_TYPE_MAP.put("short", "Short");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        // 获取用户当前编辑器对象
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (null == editor) {
            // 当前没有选中
            return;
        }

        // 通过编辑器获取用户选择对象
        SelectionModel selectionModel = editor.getSelectionModel();

        // 用户选中的文本
        String text = selectionModel.getSelectedText();

        if (StringUtils.isBlank(text)) {
            return;
        }

        // 规则校验
        if (!RestUtils.validator(text)) {
            // 不符合格式
            RestUtils.showPopup(editor, "命令不符合格式\n" +
                    "eg: getUserByIdOrName(int$id,string#name).get\n" +
                    "getUserByIdOrName -> 方法名/rest地址\n" +
                    "$/# -> url参数/表单参数\n" +
                    "get -> get请求(get|post|put|delete)");
            return;
        }
        // 生成代码
        String methodString = createMethod(text);

        // 替换文本
        replaceSelectText(e, editor, selectionModel, methodString);
    }

    /**
     * 替换选中文本
     */
    private void replaceSelectText(AnActionEvent e, Editor editor, SelectionModel selectionModel, String methodString) {
        Document document = editor.getDocument();
        Runnable runnable = () -> {
            String selectText = selectionModel.getSelectedText();
            if (selectText != null) {
                document.deleteString(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
                try {
                    document.insertString(selectionModel.getSelectionStart(), methodString);
                } catch (PluginException var3) {
                    log.error("无法替换文本信息", var3);
                    RestUtils.showPopup(editor, "无法替换文本");
                }
            }
        };
        WriteCommandAction.runWriteCommandAction(e.getData(PlatformDataKeys.PROJECT), runnable);
    }

    /**
     * 创建方法文本
     */
    private static String createMethod(String text) {

        int firstIndex = StringUtils.indexOf(text, "(");
        int lastIndex = StringUtils.lastIndexOf(text, ".") + 1;

        // 获取方法名
        String methodName = StringUtils.substring(text, 0, firstIndex);
        // 请求方式
        String requestMethod = StringUtils.substring(text, lastIndex);
        // 参数
        String argsStr = StringUtils.substring(text, firstIndex+1, lastIndex-2);
        // 转换参数
        StringBuilder path = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        translationArgsToString(argsStr, path, builder);
        String argsList = builder.substring(0, builder.length() - 2);

        // 方法模板
        return StringUtils.replaceEach(METHOD_TEMPLATE,
                new String[] {
                        "${requestMethod}",
                        "${path}",
                        "${pathArgs}",
                        "${methodName}",
                        "${args}",
                },
                new String[] {
                        REQUEST_METHOD_MAP.get(requestMethod),
                        methodName,
                        path.toString(),
                        methodName,
                        argsList
                });
    }

    /**
     * 转换规则文本到参数列表字符串
     */
    private static void translationArgsToString(String argsStr, StringBuilder path, StringBuilder builder) {

        String[] argsArr = StringUtils.split(argsStr, ",");
        if (argsArr.length > 0) {
            for (String arg : argsArr) {
                if (StringUtils.contains(arg, "#")) {
                    // 表单参数
                    String[] argNameAndType = StringUtils.split(arg, "#");
                    // 先转小写拿配置中的java原生类型
                    String argName = JAVA_TYPE_MAP.getOrDefault(StringUtils.lowerCase(argNameAndType[0]), StringUtils.capitalize(argNameAndType[0]));
                    builder.append(argName)
                            .append(" ").append(argNameAndType[1]).append(", ");

                } else if (StringUtils.contains(arg, "$")) {
                    // URL参数
                    String[] argNameAndType = StringUtils.split(arg, "$");
                    // 先转小写拿配置中的java原生类型
                    String argName = JAVA_TYPE_MAP.getOrDefault(StringUtils.lowerCase(argNameAndType[0]), StringUtils.capitalize(argNameAndType[0]));
                    builder.append("@PathVariable ").append(argName)
                            .append(" ").append(argNameAndType[1]).append(", ");
                    path.append("/{").append(argNameAndType[1]).append("}");
                }
            }
        }
    }


}
