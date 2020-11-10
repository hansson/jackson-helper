package jacksonhelper.handlers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class GenerateJacksonHandler extends AbstractHandler {

	private static final String JSON_CREATOR_IMPORT = "import com.fasterxml.jackson.annotation.JsonCreator;";
	private static final String JSON_PROPERTY_IMPORT = "import com.fasterxml.jackson.annotation.JsonProperty;";

	private static final String FIELD_PREFIX = "\tprivate final ";

	// 1 line-endings
	// 2 params
	// 3 typeName
	// 4 assignments
	private static final String CONSTRUCTOR_TEMPLATE = "\t@JsonCreator%1$s" +
			"\t%3$s(%2$s) {%1$s" +
			"%4$s%1$s" +
			"\t}%1$s";
	private static final String CONSTRUCTOR_PARAMETER_TEMPLATE = "@JsonProperty(value =\"%2$s\", required = false) %1$s %2$s";
	private static final String CONSTRUCTOR_FIELD_ASSIGNMENT_TEMPLATE = "\t\tthis.%1$s = %1$s;";
	// 1 type
	// 2 name capitalized
	// 3 line-endings
	// 4 name
	private static final String SETTER_TEMPLATE = "\tpublic %1$s get%2$s() {%3$s\t\treturn this.%4$s;%3$s\t}";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = ((IWorkbenchPage) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()).getActiveEditor();

		IEditorInput input = (IEditorInput) editor.getEditorInput();

		org.eclipse.jface.text.IDocument document = ((org.eclipse.ui.texteditor.ITextEditor) editor).getDocumentProvider()
				.getDocument(input);

		String contents = document.get();
		String lineEndings = "\n";
		if (contents.contains("\r\n")) {
			lineEndings = "\r\n";
		}

		List<String> lines = new LinkedList<>(Arrays.asList(contents.split(lineEndings)));

		boolean creatorFound = false;
		boolean propertyFound = false;
		String type = HandlerUtil.getActiveEditor(event).getEditorInput().getName().split(".java")[0];

		for (String line : lines) {
			if (line.equals(JSON_CREATOR_IMPORT)) {
				creatorFound = true;
			} else if (line.equals(JSON_PROPERTY_IMPORT)) {
				propertyFound = true;
			}

			if (creatorFound && propertyFound) {
				break;
			}

			if (line.contains("class " + type)) {
				break;
			}

		}

		if (!creatorFound) {
			lines.add(2, JSON_CREATOR_IMPORT);
		}

		if (!propertyFound) {
			lines.add(2, JSON_PROPERTY_IMPORT);
		}

		int lastFieldLine = 0;
		List<String> params = new LinkedList<>();
		List<String> assignments = new LinkedList<>();
		List<String> setters = new LinkedList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith(FIELD_PREFIX)) {
				lastFieldLine = i;
				String[] typeName = line.substring(15).split(" ");
				String fieldType = typeName[0];
				String name = typeName[1].replaceAll(";", "");
				params.add(String.format(CONSTRUCTOR_PARAMETER_TEMPLATE, fieldType, name));
				assignments.add(String.format(CONSTRUCTOR_FIELD_ASSIGNMENT_TEMPLATE, name));
				setters.add(String
						.format(SETTER_TEMPLATE, fieldType, name.substring(0, 1).toUpperCase() + name.substring(1), lineEndings, name));
			}
		}

		String constructor = String.format(CONSTRUCTOR_TEMPLATE,
				lineEndings,
				params.stream().collect(Collectors.joining("," + lineEndings + "\t\t\t")),
				type,
				assignments.stream().collect(Collectors.joining(lineEndings)));

		lastFieldLine++;
		lines.add(lastFieldLine, "");
		lastFieldLine++;
		lines.add(lastFieldLine, constructor);
		lastFieldLine++;
		lines.add(lastFieldLine, setters.stream().collect(Collectors.joining(lineEndings + lineEndings)));

		document.set(lines.stream().collect(Collectors.joining(lineEndings)));
		return null;
	}
}
