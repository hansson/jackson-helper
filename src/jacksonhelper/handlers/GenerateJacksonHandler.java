package jacksonhelper.handlers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
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
	private static final String GETTER_TEMPLATE = "\tpublic %1$s get%2$s() {%3$s\t\treturn this.%4$s;%3$s\t}";

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

		String CONSTRUCTOR_PARAM_JOIN = "," + lineEndings + "\t\t\t";

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
		List<String> getters = new LinkedList<>();
		List<String> fields = new LinkedList<>();
		int constructorExists = -1;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith(FIELD_PREFIX)) {
				lastFieldLine = i;
				String[] typeName = line.substring(15).split(" ");
				String fieldType = typeName[0];
				String name = typeName[1].replaceAll(";", "");
				fields.add(fieldType + " " + name);
				params.add(String.format(CONSTRUCTOR_PARAMETER_TEMPLATE, fieldType, name));
				assignments.add(String.format(CONSTRUCTOR_FIELD_ASSIGNMENT_TEMPLATE, name));
				String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
				if (!contents.contains("get" + capitalizedName + "()")) {
					getters.add(String
							.format(GETTER_TEMPLATE, fieldType, capitalizedName, lineEndings, name));
				}
			}
			if (line.contains("@JsonCreator")) {
				constructorExists = i;
			}
		}

		if (constructorExists == -1) {
			handleFreshFile(document, lineEndings, CONSTRUCTOR_PARAM_JOIN, lines, type, lastFieldLine, params, assignments, getters);
		} else {
			handleDirtyFile(document, lineEndings, CONSTRUCTOR_PARAM_JOIN, lines, getters, constructorExists, fields);
		}
		return null;
	}

	private void handleFreshFile(org.eclipse.jface.text.IDocument document,
			String lineEndings,
			String CONSTRUCTOR_PARAM_JOIN,
			List<String> lines,
			String type,
			int lastFieldLine,
			List<String> params,
			List<String> assignments,
			List<String> getters) {
		String constructor = String.format(CONSTRUCTOR_TEMPLATE,
				lineEndings,
				params.stream().collect(Collectors.joining(CONSTRUCTOR_PARAM_JOIN)),
				type,
				assignments.stream().collect(Collectors.joining(lineEndings)));

		lastFieldLine++;
		lines.add(lastFieldLine, "");
		lastFieldLine++;
		lines.add(lastFieldLine, constructor);
		lastFieldLine++;
		lines.add(lastFieldLine, getters.stream().collect(Collectors.joining(lineEndings + lineEndings)));
		document.set(lines.stream().collect(Collectors.joining(lineEndings)));
	}

	private void handleDirtyFile(IDocument document,
			String lineEndings,
			String CONSTRUCTOR_PARAM_JOIN,
			List<String> lines,
			List<String> getters,
			int constructorExists,
			List<String> fields) {
		Constructor constructor = parseConstructor(lineEndings, lines, constructorExists);

		List<String> missingFields = fields.stream()
				.filter(field -> !constructor.getHeader().contains(field))
				.filter(field -> !constructor.getHeader().contains("Optional<" + field.split(" ")[0] + "> " + field.split(" ")[1]))
				.collect(Collectors.toList());
		if (missingFields.size() == 0) {
			return;
		}

		String header = createHeader(CONSTRUCTOR_PARAM_JOIN, constructor, missingFields);
		String body = createBody(lineEndings, constructor, missingFields);
		String completeConstructor = header + body;

		List<String> beforeConstructor = lines.subList(0, constructor.getStartLine());
		List<String> afterConstructor = lines.subList(constructor.getEndLine() + 1, lines.size());
		String newGetters = getters.stream().collect(Collectors.joining(lineEndings + lineEndings));
		document.set(
				beforeConstructor.stream().collect(Collectors.joining(lineEndings))
						+ lineEndings
						+ completeConstructor
						+ lineEndings
						+ newGetters
						+ lineEndings
						+ afterConstructor.stream().collect(Collectors.joining(lineEndings)));

	}

	private String createBody(String lineEndings, Constructor constructor, List<String> missingFields) {
		int lastAssignment = constructor.getBody().lastIndexOf(";") + 1;
		String bodyFirstPart = constructor.getBody().substring(0, lastAssignment);
		String bodyLastPart = constructor.getBody().substring(lastAssignment);
		String bodyMiddlePart = missingFields.stream().map(field -> {
			String[] typeField = field.split(" ");
			return String.format(CONSTRUCTOR_FIELD_ASSIGNMENT_TEMPLATE, typeField[1]);
		}).collect(Collectors.joining(lineEndings));
		String body = bodyFirstPart + lineEndings + bodyMiddlePart + bodyLastPart;
		return body;
	}

	private String createHeader(String CONSTRUCTOR_PARAM_JOIN, Constructor constructor, List<String> missingFields) {
		int lastParameterInsert = constructor.getHeader().lastIndexOf(")");
		String headerFirstPart = constructor.getHeader().substring(0, lastParameterInsert);
		String headerLastPart = constructor.getHeader().substring(lastParameterInsert);
		String headerMiddlePart = missingFields.stream().map(field -> {
			String[] typeField = field.split(" ");
			return String.format(CONSTRUCTOR_PARAMETER_TEMPLATE, typeField[0], typeField[1]);
		}).collect(Collectors.joining(CONSTRUCTOR_PARAM_JOIN));
		String header = headerFirstPart + CONSTRUCTOR_PARAM_JOIN + headerMiddlePart + headerLastPart;
		return header;
	}

	private Constructor parseConstructor(String lineEndings, List<String> lines, int constructorExists) {
		String constructorHeader = "";
		int[] constructorHeaderLines = new int[2];
		String constructorBody = "";
		int[] constructorBodyLines = new int[2];
		int openCurlyBrackets = 1;
		constructorHeaderLines[0] = constructorExists + 1;
		for (int i = constructorHeaderLines[0]; i < lines.size(); i++) {
			String line = lines.get(i);
			if (constructorHeaderLines[1] == 0) {
				constructorHeader = constructorHeader + line + lineEndings;
				if (line.contains("{")) {
					constructorHeaderLines[1] = i;
				}
			} else {
				constructorBody = constructorBody + line + lineEndings;
				if (constructorBodyLines[0] == 0) {
					constructorBodyLines[0] = i;
				}
				if (line.contains("{")) {
					openCurlyBrackets++;
				}

				if (line.contains("}")) {
					openCurlyBrackets--;
				}

				if (openCurlyBrackets == 0) {
					constructorBodyLines[1] = i;
					break;
				}
			}
		}
		Constructor constructor = new Constructor(constructorHeader, constructorBody, constructorHeaderLines[0], constructorBodyLines[1]);
		return constructor;
	}

}
