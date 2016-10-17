import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.net.URL;

/**
 * Created by seusher on 10/16/16.
 */
public class DictionaryTreeCellRenderer implements TreeCellRenderer {
    private JLabel label;

    private ImageIcon entityIcon;
    private ImageIcon datasetIcon;
    private ImageIcon columnIcon;

    DictionaryTreeCellRenderer() {
        label = new JLabel();

        entityIcon = this.loadIcon("entity.png");
        datasetIcon = this.loadIcon("dataset.png");
        columnIcon = this.loadIcon("column.png");
    }

    private ImageIcon loadIcon(String imageName) {
        URL imageUrl = getClass().getResource("/images/" + imageName);
        ImageIcon icon = new ImageIcon(imageUrl);

        return icon;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        Object o = ((DefaultMutableTreeNode) value).getUserObject();
        if (o instanceof DataDictionaryBrowserFactory.TreeViewEntry) {

            DataDictionaryBrowserFactory.TreeViewEntry entry = (DataDictionaryBrowserFactory.TreeViewEntry) o;

            ImageIcon icon;

            switch (entry.getType()) {
                case Dataset:
                    icon = datasetIcon;
                    break;
                case Column:
                    icon = columnIcon;
                    break;
                default:
                    icon = entityIcon;
            }

            label.setIcon(icon);
            label.setText(entry.getName());
        } else {
            label.setIcon(null);
            label.setText("" + value);
        }
        return label;
    }
}
