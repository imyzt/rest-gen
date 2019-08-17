package top.imyzt.plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author imyzt
 * @date 2019/08/17
 * @description 工具类
 */
public class RestUtils {

    /**
     * 校验文本格式是否符合生成条件
     */
    public static boolean validator(String text) {
        String regex = "\\S+\\((\\S*\\$\\S*|\\S*#\\S*)\\)\\.(get|post|put|delete)";
        // 忽略大小写
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    /**
     * 显示消息到选中文本下方
     */
    public static void showPopup(Editor editor, String showText) {
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
