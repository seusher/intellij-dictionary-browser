import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import com.sun.org.apache.bcel.internal.generic.POP;
import org.apache.commons.lang.enums.Enum;
import org.apache.sanselan.common.ImageMetadata;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

public class DataDictionaryBrowserFactory implements ToolWindowFactory {

    private JPanel dictionaryBrowserWindowContent;
    private JTree dictionaryTree;
    private JTextField searchField;
    private TextFieldWithBrowseButton dictionaryLocationTextField;
    private JPanel entityPanel;
    private JTextField txtEntityName;
    private JTextPane txtEntityDesc;
    private JPanel datasetPanel;
    private JTextField txtDatasetName;
    private JTextPane txtDatasetDesc;
    private JTextField txtTableName;
    private JPanel emptyPanel;
    private JPanel metadataPanel;
    private JPanel columnPanel;
    private JTextField txtColumnName;
    private JTextField textField2;
    private JTextPane txtColumnDesc;
    private ToolWindow dictionaryBrowserToolWindow;

    public DataDictionaryBrowserFactory() {

        dictionaryTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);

                // Get the path to the selected item
                TreePath selectionPath = dictionaryTree.getSelectionPath();

                int pathCount = selectionPath.getPathCount();

                // Path Counts:
                // 1 = Root (Entities node)
                // 2 = Entity
                // 3 = Dataset
                // 4 = Column

                CardLayout layout = (CardLayout)metadataPanel.getLayout();

                if (pathCount == 2) {
                    layout.show(metadataPanel, "Entity");
                }
                else if (pathCount == 3) {
                    layout.show(metadataPanel, "Dataset");
                }
                else if (pathCount == 4) {
                    layout.show(metadataPanel, "Column");
                }
                else
                    layout.show(metadataPanel, "Empty");

                // If we select an Entity or the root node, don't do anything
                if (pathCount < 3)
                    return;

                // Only respond to double clicks
                if (mouseEvent.getClickCount() < 2)
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

                    final Runnable readRunner = () -> editor.getDocument().insertString(caretOffset, ((TreeViewEntry)targetNode.getUserObject()).getName());

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
        dictionaryLocationTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                // Get the path for the dictionary and load it.
                String path = dictionaryLocationTextField.getText();
                PopulateListView(path);
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

        dictionaryTree.setCellRenderer(new DictionaryTreeCellRenderer());

        // Only allow selecting folders
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        dictionaryLocationTextField.addBrowseFolderListener("Data Dictionary Directory",
                                                            "Select data dictionary directory",
                                                            null,
                                                            descriptor);

        // Ensure that we don't show the default treeview items by default.
        // The new default should instruct the user to select a folder for the dictionary.
        TreeNode defaultNode = new DefaultMutableTreeNode("Select a data dictionary folder location.");
        DefaultTreeModel defaultModel = new DefaultTreeModel(defaultNode);
        dictionaryTree.setModel(defaultModel);
    }

    private void PopulateListView(String path)
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Entities");

        // Load entities from data dictionary
        DefaultMutableTreeNode entity = new DefaultMutableTreeNode(new TreeViewEntry("Tenant", ItemType.Entity));
        root.add(entity);

        DefaultMutableTreeNode dataset = new DefaultMutableTreeNode(new TreeViewEntry("Insight", ItemType.Dataset));

        DefaultMutableTreeNode column1 = new DefaultMutableTreeNode(new TreeViewEntry("tenant_id", ItemType.Column));
        DefaultMutableTreeNode column2 = new DefaultMutableTreeNode(new TreeViewEntry("insight1", ItemType.Column));

        dataset.add(column1);
        dataset.add(column2);

        entity.add(dataset);

        DefaultTreeModel model = (DefaultTreeModel)dictionaryTree.getModel();

        model.setRoot(root);
        model.reload();
    }

    class TreeViewEntry
    {
        private String name;
        public ItemType type;

        public TreeViewEntry(String name, ItemType type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return this.name; }
        public ItemType getType() { return this.type; }
    }

    public enum ItemType {
        Entity,
        Dataset,
        Column
    }
}