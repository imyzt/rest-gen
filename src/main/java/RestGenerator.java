import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestGenerator extends AnAction {

    private static final Logger log = LoggerFactory.getLogger(RestGenerator.class);

    private static final String METHOD_TEMPLATE = "${requestMethod}(\"${path}${pathArgs}\")\n" +
            "\tpublic void ${methodName}(${args}) {\n" +
            "\t\t\n" +
            "\t}";

    private static final Map<String, String> REQUEST_METHOD_MAP = new HashMap<>(4);
    static {
        REQUEST_METHOD_MAP.put("get", "@GetMapping");
        REQUEST_METHOD_MAP.put("post", "@PostMapping");
        REQUEST_METHOD_MAP.put("put", "@PutMapping");
        REQUEST_METHOD_MAP.put("delete", "@DeleteMapping");
    }

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
        if (!validator(text)) {
            // 不符合格式
            showPopup(editor, "命令不符合格式\n" +
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

    private boolean validator(String text) {
        String regex = "\\S+\\((\\S*\\$\\S*|\\S*#\\S*)\\)\\.(get|post|put|delete)";
        // 忽略大小写
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

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
                    showPopup(editor, "无法替换文本");
                }
            }
        };
        WriteCommandAction.runWriteCommandAction(e.getData(PlatformDataKeys.PROJECT), runnable);
    }

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

    private void showPopup(Editor editor, String showText) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 获取默认popup工厂
            JBPopupFactory popupFactory = JBPopupFactory.getInstance();
            BalloonBuilder builder = popupFactory.createHtmlTextBalloonBuilder(showText, null,
                    new JBColor(new Color(188, 238, 188), new Color(73, 120, 73)), null);

            builder.setFadeoutTime(10000) // 10秒无操作
                    .createBalloon()  // 创建气泡
                    .show(popupFactory.guessBestPopupLocation(editor), Balloon.Position.below); //指定位置(editor对象即目前选中的内容),并显示在下方
        });
    }
}
