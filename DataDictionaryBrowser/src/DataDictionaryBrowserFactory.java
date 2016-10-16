import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.*;

public class DataDictionaryBrowserFactory implements ToolWindowFactory {

    private JPanel dictionaryBrowserWindowContent;
    private JTree dictionaryTree;
    private JTextField searchField;
    private ToolWindow dictionaryBrowserToolWindow;

    public DataDictionaryBrowserFactory() {

        dictionaryTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);

                // Only respond to double clicks
                if (mouseEvent.getClickCount() < 2)
                    return;

                // Get the path to the selected item
                TreePath selectionPath = dictionaryTree.getSelectionPath();

                int pathCount = selectionPath.getPathCount();

                // Path Counts:
                // 1 = Root (Entities node)
                // 2 = Entity
                // 3 = Dataset
                // 4 = Column

                // If we select an Entity or the root node, don't do anything
                if (pathCount < 3)
                    return;

                // If we selected a column, set the context to its dataset
                if (pathCount > 3)
                    selectionPath = selectionPath.getParentPath();

                final DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();

                // Get the context, which is needed to retrieve the project
                DataContext context = DataManager.getInstance().getDataContextFromFocus().getResultSync();
                Project project = DataKeys.PROJECT.getData(context);

                // Using the project, we can get the active text editor which we need to write to
                Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                int caretOffset = editor.getCaretModel().getCurrentCaret().getOffset();

                // All write operations need to be executed via a runWriteAction call
                ApplicationManager.getApplication().invokeLater(() -> CommandProcessor.getInstance().executeCommand(project, () -> {

                    final Runnable readRunner = () -> editor.getDocument().insertString(caretOffset, targetNode.getUserObject().toString());

                    ApplicationManager.getApplication().runWriteAction(readRunner);
                }, "DataDictionary", null));
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                super.keyTyped(keyEvent);

                // Filter the dictionary tree view
            }
        });
    }

    // Create the tool window content.
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        dictionaryBrowserToolWindow = toolWindow;

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(dictionaryBrowserWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Entities");

        // Load entities from data dictionary
        DefaultMutableTreeNode entity = new DefaultMutableTreeNode("Tenant");
        root.add(entity);

        DefaultMutableTreeNode dataset = new DefaultMutableTreeNode("Insight");

        DefaultMutableTreeNode column1 = new DefaultMutableTreeNode("tenant_id");
        DefaultMutableTreeNode column2 = new DefaultMutableTreeNode("insight1");

        dataset.add(column1);
        dataset.add(column2);

        entity.add(dataset);

        DefaultTreeModel model = (DefaultTreeModel)dictionaryTree.getModel();

        model.setRoot(root);
        model.reload();
    }
}