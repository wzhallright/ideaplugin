import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自己的代码仓库不能上传company的相关ip或者地址信息，所以
 * 本插件暂时只支持从交互界面中传入NN的IP信息
 */
public class UpLineTool extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
    MyDialog myDialog = new MyDialog(anActionEvent.getProject(),"NN 上线");
    myDialog.show();
    myDialog.setResizable(true);
//    myDialog.setSize(600, 300);

//    instance.createComponentPopupBuilder(jPanel, new JBLabel())//参数说明：内容对象,优先获取
//        .setTitle("上线检查")
//        .setMovable(true)
//        .setResizable(true)
//        .setNormalWindowLevel(false)
//        .setMinSize(new Dimension(1600, 300))
//        .createPopup()
//        .showInFocusCenter();
  }

  class MyDialog extends DialogWrapper {

    public MyDialog(Project project,String title) {
      super(project);
      init();
      setTitle(title);
      setSize(600, 300);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      JPanel jPanel = new JPanel();
      JButton button = new JButton("检测");
      JTextField testField = new JTextField(16);
      jPanel.add(button);
      jPanel.add(testField);
      button.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String ip = testField.getText();
          String content = null;
          String content1 = null;
          JSONObject request;
          JSONObject request1;
          String url = "http://" + ip + ":50070/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem";
          String url1 = "http://" + ip + ":50070/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo";
          try {
            content = HttpProxy.sendGet(url);
            content1 = HttpProxy.sendGet(url1);
            String nsid = null;
            long lastLeaveSafeModeTime = -1;
            String status = null;
            long NNStartedTimeInMillis = -1;
            String version = null;
            if(content != null) {
              request = JSON.parseObject(content);
              JSONArray jsonArray = (JSONArray) request.get("beans");
              JSONObject jsonObject = (JSONObject) jsonArray.get(0);
              lastLeaveSafeModeTime = (Long) jsonObject.get("LastLeaveSafeModeTime");
              nsid = (String) jsonObject.get("tag.NsId");
              status = (String) jsonObject.get("tag.HAState");
            }
            if (content1 != null) {
              request1 = JSON.parseObject(content1);
              JSONArray jsonArray = (JSONArray) request1.get("beans");
              JSONObject jsonObject = (JSONObject) jsonArray.get(0);
              NNStartedTimeInMillis = (Long) jsonObject.get("NNStartedTimeInMillis");
              version = (String) jsonObject.get("Version");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String startedTime = sdf.format(new Date(NNStartedTimeInMillis));
            String lastLeaveSafeModeTimeStr = sdf.format(new Date(lastLeaveSafeModeTime));

//            JLabel nsLabel = new JLabel("ns");
//            JTextField nsTextField = new JTextField();
//            nsTextField.setText(nsid);
//            jPanel.add(nsLabel);
//            jPanel.add(nsTextField);
//            JLabel statusLabel = new JLabel("当前状态");
//            JTextField statusTextField = new JTextField();
//            statusTextField.setText(status);
//            jPanel.add(statusLabel);
//            jPanel.add(statusTextField);
//            JLabel startLabel = new JLabel("启动时间");
//            JTextField startTextField = new JTextField();
//            startTextField.setText(startedTime);
//            jPanel.add(startLabel);
//            jPanel.add(startTextField);
//            JLabel safeModeLabel = new JLabel("退出安全模式时间");
//            JTextField safeModeTextField = new JTextField();
//            safeModeTextField.setText(lastLeaveSafeModeTimeStr);
//            jPanel.add(safeModeLabel);
//            jPanel.add(safeModeTextField);
//            JLabel failOverLabel = new JLabel("是否建议切换");
//            JTextField failOverTextField = new JTextField();
//            failOverTextField.setText("可以切换");
//            jPanel.add(failOverLabel);
//            jPanel.add(failOverTextField);
            // 构造一个table
          String[] names = {"nsid", "当前状态", "版本号", "启动时间", "退出安全模式时间", "是否建议切换"};
          Object[][] columns = new Object[1][6];
          columns[0][0] = nsid;
          columns[0][1] = status;
          columns[0][2] = version;
          columns[0][3] = startedTime;
          columns[0][4] = lastLeaveSafeModeTimeStr;
          if (!"standby".equals(status)) {
            columns[0][5] = "非standby,不能切换";
          } else if ((System.nanoTime() - lastLeaveSafeModeTime) > 1800000) {
            columns[0][5] = "可以切换";
          } else {
            columns[0][5] = "再等等";
          }
          JTable table = new JTable(columns, names);
          jPanel.add(new JScrollPane(table), BorderLayout.CENTER);
          } catch (IOException exception) {
            Notifications.Bus.notify(
                new Notification("xtools", "结果", "获取信息异常", NotificationType.INFORMATION)
            );
            exception.printStackTrace();
          }
        }
      });
      return jPanel;
    }
  }

}
