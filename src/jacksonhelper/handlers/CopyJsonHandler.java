package jacksonhelper.handlers;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import jackson_helper.JacksonHelperPlugin;

public class CopyJsonHandler extends AbstractHandler {

	private static final String FIELD_PREFIX = "\tprivate final ";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PlatformUI.getPreferenceStore();

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

		Map<String, String> fields = new HashMap<>();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith(FIELD_PREFIX)) {
				String[] typeName = line.substring(FIELD_PREFIX.length()).split(" ");
				String fieldType = typeName[0];
				String name = typeName[1].replaceAll(";", "");
				fields.put(name, fieldType);
			}
		}

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		StringSelection strSel = new StringSelection(fieldsToJson(fields));
		clipboard.setContents(strSel, null);

		return null;
	}

	private String fieldsToJson(Map<String, String> fields) {
		String result = "{\n\t";
		result += fields.entrySet().stream().map(entry -> {
			if (isCustomType(entry.getValue())) {
				return "\"" + entry.getKey() + "\": " + customTypeValue(entry.getValue()) + "";
			}

			if (isNumber(entry.getValue())) {
				return "\"" + entry.getKey() + "\": 0";
			}

			if (isBoolean(entry.getValue())) {
				return "\"" + entry.getKey() + "\": false";
			}

			if (isString(entry.getValue())) {
				return "\"" + entry.getKey() + "\": \"\"";
			}

			if (isList(entry.getValue())) {
				return "\"" + entry.getKey() + "\": []";
			}

			if (isLocalDateTime(entry.getValue())) {
				return "\"" + entry.getKey() + "\": \"" + LocalDateTime.now() + "\"";
			}

			if (isDate(entry.getValue())) {
				return "\"" + entry.getKey() + "\": \"" + LocalDate.now() + "\"";
			}

			if (isOffsetDateTime(entry.getValue())) {
				return "\"" + entry.getKey() + "\": \"" + OffsetDateTime.now() + "\"";
			}
			return "\"" + entry.getKey() + "\": {}";
		}).collect(Collectors.joining(",\n\t"));
		return result + "\n}";
	}

	private String customTypeValue(String value) {
		return JacksonHelperPlugin.getDefault().getCustomTypesPreference()
				.stream()
				.filter(customType -> customType.startsWith(value + " = "))
				.map(s -> s.split(" = ")[1])
				.findFirst().orElseThrow();
	}

	private boolean isCustomType(String value) {
		return JacksonHelperPlugin.getDefault().getCustomTypesPreference().stream().map(s -> s.split(" = ")[0])
				.anyMatch(customType -> customType.equals(value));
	}

	private boolean isBoolean(String value) {
		return value.equals("boolean") || value.equals("Boolean");
	}

	private boolean isDate(String value) {
		return value.equals("LocalDate");
	}

	private boolean isLocalDateTime(String value) {
		return value.equals("LocalDateTime");
	}

	private boolean isOffsetDateTime(String type) {
		return type.equals("OffsetDateTime");
	}

	private boolean isList(String type) {
		return type.startsWith("List<") || type.startsWith("Set<");
	}

	private boolean isString(String type) {
		return type.equals("String");
	}

	private boolean isNumber(String type) {
		return type.equals("int") || type.equals("Integer") || type.equals("BigInteger") || type.equals("double") || type.equals("Double")
				|| type.equals("float") || type.equals("Float") || type.equals("BigDecimal") || type.equals("long") || type.equals("Long");
	}

}
