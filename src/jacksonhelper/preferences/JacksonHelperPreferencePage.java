package jacksonhelper.preferences;

import java.util.Arrays;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import jackson_helper.JacksonHelperPlugin;

public class JacksonHelperPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private List customTypes;

	private Text newType;
	private Text newValue;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(JacksonHelperPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite composite) {
		Composite entryTable = new Composite(composite, SWT.NULL);

		// Create a data that takes up the extra space in the dialog .
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);

		// Add in a dummy label for spacing
		Label label = new Label(entryTable, SWT.NONE);
		label.setText("Custom json type resolvers (Will override default resolvers)");

		customTypes = new List(entryTable, SWT.BORDER);
		customTypes.setItems(JacksonHelperPlugin.getDefault().getCustomTypesPreference().toArray(new String[0]));

		// Create a data that takes up the extra space in the dialog and spans both columns.
		data = new GridData(GridData.FILL_BOTH);
		customTypes.setLayoutData(data);

		Composite buttonComposite = new Composite(entryTable, SWT.NULL);

		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 3;
		buttonComposite.setLayout(buttonLayout);

		// Create a data that takes up the extra space in the dialog and spans both columns.
		data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		buttonComposite.setLayoutData(data);

		Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		addButton.setText("Add to List"); //$NON-NLS-1$
		addButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				customTypes.add(newType.getText() + " = " + newValue.getText(), customTypes.getItemCount());
			}
		});

		newType = new Text(buttonComposite, SWT.BORDER);
		newType.setText("Type");
		newValue = new Text(buttonComposite, SWT.BORDER);
		newValue.setText("Json value");
		// Create a data that takes up the extra space in the dialog .
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		newType.setLayoutData(data);
		newValue.setLayoutData(data);

		Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		removeButton.setText("Remove Selection"); //$NON-NLS-1$
		removeButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				customTypes.remove(customTypes.getSelectionIndex());
			}
		});

		data = new GridData();
		data.horizontalSpan = 2;
		removeButton.setLayoutData(data);

		return entryTable;
	}

	public boolean performOk() {
		JacksonHelperPlugin.getDefault().setCustomTypesPreference(Arrays.asList(customTypes.getItems()));
		return super.performOk();
	}

}
