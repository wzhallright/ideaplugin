import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class ShowCode extends AnAction {


  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    String path = project.getBasePath();
    JBPopupFactory instance = JBPopupFactory.getInstance();
    JPanel jPanel = new JPanel();
    JButton jButton = new JButton("获取");
    jButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        StringBuffer sb = new StringBuffer();
        sb.append("cd").append(" ").append(path).append(";");
        String cmd =
            "git log --format='%aN' | sort -u | while read name; do echo \"$name\"; git"
                + " log --author=\"$name\" --pretty=tformat: --numstat | awk '{ add += $1; subs += $2; loc += $1 - $2 } END { printf \"%s:%s:%s\\n\", add, subs, loc }' -; done\n";

        String cmdFinal = sb.append(cmd).toString();
        Process process = null;
        BufferedReader inputResult = null;
        try {
          process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmdFinal});
          process.waitFor();
          inputResult =
              new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line1 = "";
          LinkedList<String> listNum = new LinkedList<>();
          LinkedList<String> listName = new LinkedList<>();
          int count = 0;
          while ((line1 = inputResult.readLine()) != null) {
            if ((count % 2) == 0) {
              listName.add(line1);
            } else {
              listNum.add(line1);
            }
            count ++;
          }
          Object[][] columns = new Object[listName.size()][4];
          for (int i = 0; i < listName.size(); i++) {
            String[] str = listNum.get(i).split(":");
            if (str.length == 0) {
              columns[i][0] = listName.get(i);
              columns[i][1] = 0;
              columns[i][2] = 0;
              columns[i][3] = 0;
            } else {
              columns[i][0] = listName.get(i);
              columns[i][1] = str[0];
              columns[i][2] = str[1];
              columns[i][3] = str[2];
            }
          }

          String[] names = {"name", "added_line", "removed_line", "total_line"};
          JTable table = new JTable(columns, names);
          jPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        } catch (Exception exception) {
          exception.printStackTrace();
        } finally {
          try {
            inputResult.close();
            process.destroy();
          } catch (IOException ioException) {
            ioException.printStackTrace();
          }
        }
      }
    });

    jPanel.add(jButton, BorderLayout.NORTH);
    instance.createComponentPopupBuilder(jPanel, new JBLabel())//参数说明：内容对象,优先获取
        .setTitle("代码量")
        .setMovable(true)
        .setResizable(true)
        .setNormalWindowLevel(false)
        .setMinSize(new Dimension(600, 300))
        .createPopup()
        .showInFocusCenter();

  }
}
