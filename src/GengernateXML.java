import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class GengernateXML extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    // TODO: insert action logic here
    final VirtualFile file;
    BufferedReader bufferedReader = null;
    try {
      file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
      bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream()));
      String line = "";
      HashMap<String, String> hashMap = new HashMap<>();
      while ((line = bufferedReader.readLine()) != null) {
        String[] nameAndValue = line.split("=");
        hashMap.put(nameAndValue[0].trim(), nameAndValue[1].trim());
      }
      buildXml(hashMap, file);
    } catch (Exception exception) {
      Notifications.Bus.notify(
          new Notification("xtools", "结果", "xml生成失败", NotificationType.INFORMATION)
      );
      exception.printStackTrace();
    } finally {
      try {
        bufferedReader.close();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
    Notifications.Bus.notify(
        new Notification("xtools", "结果", "xml生成成功", NotificationType.INFORMATION)
    );
  }

  public void buildXml(HashMap<String, String> hashMap, VirtualFile file) throws Exception {
    Element root, property, name, value;
    root = new Element("configuration");
    property = new Element("property");
    name = new Element("name");
    value = new Element("value");
    Document docRoot = new Document(root);
    for (Entry<String, String> entry : hashMap.entrySet()) {
      name.setText(entry.getKey());
      value.setText(entry.getValue());
      property.addContent(name);
      property.addContent(value);
    }

    root.addContent(property);
    Format format = Format.getCompactFormat();
    format.setEncoding("UTF-8");           //设置xml文件的字符为UTF-8
    format.setIndent("    ");               //设置xml文件的缩进为4个空格
    XMLOutputter XMLOut = new XMLOutputter(format);//在元素后换行，每一层元素缩排四格
    VirtualFile outputPath = file.getParent();
    XMLOut.output(docRoot, new FileOutputStream(outputPath.getPath() +"/generate.xml"));
  }
}
