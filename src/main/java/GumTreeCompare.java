import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GumTreeCompare {
    private final static String HELP_MESSAGE = "Usage: pass two paths to files with source code to compare";

    public static void main(String[] args) {
        if (args.length != 2 || !Files.isRegularFile(Paths.get(args[0])) || !Files.isRegularFile(Paths.get(args[1]))) {
            System.out.println(HELP_MESSAGE);
            return;
        }

        Run.initGenerators();
        try {
            Generators generator = Generators.getInstance();
            TreeContext tree1 = generator.getTree(args[0]);
            TreeContext tree2 = generator.getTree(args[1]);

            System.out.println(args[0] + " abstract syntax tree:");
            System.out.println(TreeIoUtils.toXml(tree1));
            System.out.println(args[1] + " abstract syntax tree:");
            System.out.println(TreeIoUtils.toCompactXml(tree2));

            ITree root1 = tree1.getRoot();
            ITree root2 = tree2.getRoot();
            Matcher matcher = Matchers.getInstance().getMatcher(root1, root2);
            matcher.match();
            MappingStore mappingStore = matcher.getMappings();
            ActionGenerator actionGenerator = new ActionGenerator(root1, root2, mappingStore);
            actionGenerator.generate();
            List<Action> actions = actionGenerator.getActions();

            System.out.println("Differences between the ASTs:");
            printActions(tree1, mappingStore, actions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints a list of changes to the output stream in the format of ActionIoUtils.toText serializer
     * @param context the AST before the the actions
     * @param mappingStore mappings between the original and changed ASTs
     * @param actions the list of the actions
     */
    private static void printActions(TreeContext context, MappingStore mappingStore, List<Action> actions) {
        for (Action action : actions) {
            ITree sourceNode = action.getNode();
            switch (action.getName()) {
                case "MOV": {
                    System.out.printf("Move %s into %s at %d\n",
                            nodeRepr(sourceNode, context), nodeRepr(mappingStore.getDst(sourceNode), context),
                            ((Move) action).getPosition());
                    break;
                }
                case "INS": {
                    ITree destNode = action.getNode();
                    ITree parent = destNode.getParent();
                    System.out.printf("Insert %s into %s at %d\n",
                            nodeRepr(sourceNode, context), nodeRepr(parent, context), parent.getChildPosition(destNode));
                    break;
                }
                case "UPD": {
                    System.out.printf("Update %s to %s\n",
                            nodeRepr(sourceNode, context), mappingStore.getDst(sourceNode).getLabel());
                    break;
                }
                case "DEL": {
                    System.out.printf("Delete %s\n", nodeRepr(sourceNode, context));
                    break;
                }
            }
        }
    }

    private static String nodeRepr(ITree node, TreeContext context) {
        return String.format("%s(%d)", node.toPrettyString(context), node.getId());
    }
}
