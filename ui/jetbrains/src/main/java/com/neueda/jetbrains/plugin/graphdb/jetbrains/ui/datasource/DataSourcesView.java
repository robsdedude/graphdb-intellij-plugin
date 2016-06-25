package com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.datasource;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.PatchedDefaultMutableTreeNode;
import com.intellij.ui.treeStructure.Tree;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.component.datasource.DataSource;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.component.datasource.DataSourcesComponent;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.datasource.actions.RefreshDataSourcesAction;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.datasource.interactions.DataSourceInteractions;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.datasource.metadata.CypherMetadataRetriever;
import com.neueda.jetbrains.plugin.graphdb.jetbrains.ui.datasource.tree.GraphColoredTreeCellRenderer;
import com.neueda.jetbrains.plugin.graphdb.language.cypher.completion.CypherMetadataProviderService;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.GridLayout;
import java.util.Enumeration;
import java.util.List;

public class DataSourcesView implements Disposable {

    private boolean initialized;

    private DataSourcesComponent component;
    private DataSourceInteractions interactions;
    private PatchedDefaultMutableTreeNode treeRoot;
    private DefaultTreeModel treeModel;

    private JPanel toolWindowContent;
    private JPanel treePanel;
    private Tree dataSourceTree;
    private ToolbarDecorator decorator;
    private CypherMetadataProviderService cypherMetadataProviderService;
    private CypherMetadataRetriever cypherMetadataRetriever;

    public DataSourcesView() {
        initialized = false;
    }

    public void initToolWindow(Project project, ToolWindow toolWindow) {
        if (!initialized) {
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(toolWindowContent, "", false);
            toolWindow.getContentManager().addContent(content);

            component = project.getComponent(DataSourcesComponent.class);
            cypherMetadataProviderService = ServiceManager.getService(project, CypherMetadataProviderService.class);
            cypherMetadataRetriever = new CypherMetadataRetriever(project.getMessageBus(), cypherMetadataProviderService);
            treeRoot = new PatchedDefaultMutableTreeNode("treeRoot");
            treeModel = new DefaultTreeModel(treeRoot, false);
            decorator = ToolbarDecorator.createDecorator(dataSourceTree);
            decorator.addExtraAction(new RefreshDataSourcesAction(this));

            configureDataSourceTree();
            decorateDataSourceTree();

            interactions = new DataSourceInteractions(project, this);

            replaceTreeWithDecorated();
            showDataSources();
            refreshDataSourcesMetadata();

            initialized = true;
        }
    }

    public DataSourcesComponent getComponent() {
        return component;
    }

    public Tree getDataSourceTree() {
        return dataSourceTree;
    }

    public PatchedDefaultMutableTreeNode getTreeRoot() {
        return treeRoot;
    }

    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    public ToolbarDecorator getDecorator() {
        return decorator;
    }

    private void createUIComponents() {
        treePanel = new JPanel(new GridLayout(0, 1));
    }

    private void configureDataSourceTree() {
        dataSourceTree.getEmptyText().setText("Create a data source");
        dataSourceTree.setCellRenderer(new GraphColoredTreeCellRenderer(component));
        dataSourceTree.setModel(treeModel);
        dataSourceTree.setRootVisible(false);
    }

    private void decorateDataSourceTree() {
        decorator.setPanelBorder(BorderFactory.createEmptyBorder());
        decorator.setToolbarPosition(ActionToolbarPosition.TOP);
    }

    private void replaceTreeWithDecorated() {
        JPanel panel = decorator.createPanel();
        treePanel.add(panel);
    }

    private void showDataSources() {
        component.getDataSources()
                .forEach((dataSource) -> treeRoot.add(new PatchedDefaultMutableTreeNode(dataSource)));
        treeModel.reload();
    }

    public void refreshDataSourcesMetadata() {
        Enumeration children = treeRoot.children();
        boolean isRefreshed = false;
        while (children.hasMoreElements()) {
            if (refreshDataSourceMetadata((PatchedDefaultMutableTreeNode) children.nextElement())) {
                isRefreshed = true;
            }
        }

        if (isRefreshed) {
            treeModel.reload();
        }
    }

    public boolean refreshDataSourceMetadata(PatchedDefaultMutableTreeNode treeNode) {
        DataSource nodeDataSource = (DataSource) treeNode.getUserObject();
        return cypherMetadataRetriever.refresh(treeNode, nodeDataSource);
    }

    public void createDataSource(DataSource dataSource) {
        component.addDataSource(dataSource);
        PatchedDefaultMutableTreeNode treeNode = new PatchedDefaultMutableTreeNode(dataSource);
        treeRoot.add(treeNode);
        refreshDataSourceMetadata(treeNode);
        treeModel.reload();
    }

    public void updateDataSource(PatchedDefaultMutableTreeNode treeNode, DataSource oldDataSource, DataSource newDataSource) {
        component.updateDataSource(oldDataSource, newDataSource);
        treeNode.setUserObject(newDataSource);
        refreshDataSourceMetadata(treeNode);
        treeModel.reload();
    }

    public void removeDataSources(List<DataSource> dataSourcesForRemoval) {
        component.removeDataSources(dataSourcesForRemoval);

        dataSourcesForRemoval.stream()
                .map(DataSource::getName)
                .map(name -> {
                    Enumeration enumeration = treeRoot.children();
                    while (enumeration.hasMoreElements()) {
                        DefaultMutableTreeNode element = (DefaultMutableTreeNode) enumeration.nextElement();
                        DataSource dataSource = (DataSource) element.getUserObject();
                        if (dataSource.getName().equals(name)) {
                            return element;
                        }
                    }
                    return null;
                })
                .filter(ds -> ds != null)
                .forEach(treeRoot::remove);

        treeModel.reload();
    }

    @Override
    public void dispose() {
    }
}