/*
 * 11/14/2003
 *
 * FindDialog - Dialog for finding text in a GUI.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.rsta.ui.AssistanceIconPanel;
import org.fife.rsta.ui.ResizableFrameContentPane;
import org.fife.rsta.ui.UIUtil;
import org.fife.ui.rtextarea.SearchEngine;


/**
 * A "Find" dialog similar to those found in most Windows text editing
 * applications.  Contains many search options, including:<br>
 * <ul>
 *   <li>Match Case
 *   <li>Match Whole Word
 *   <li>Use Regular Expressions
 *   <li>Search Forwards or Backwards
 *   <li>Mark all
 * </ul>
 * The dialog also remembers your previous several selections in a combo box.
 * <p>An application can use a <code>FindDialog</code> as follows.  It is
 * suggested that you create an <code>Action</code> or something similar to
 * facilitate "bringing up" the Find dialog.  Have the main application contain
 * an object that implements both <code>ActionListener</code>.  This object will
 * receive the following events from the Find dialog:
 * <ul>
 *   <li>{@link AbstractFindReplaceDialog#ACTION_FIND ACTION_FIND} action when
 *       the user clicks the "Find" button.
 * </ul>
 * The application can then call i.e.
 * {@link SearchEngine#find(javax.swing.JTextArea, org.fife.ui.rtextarea.SearchContext) SearchEngine.find()}
 * to actually execute the search.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FindDialog extends AbstractFindReplaceDialog implements ActionListener {

	private static final long serialVersionUID = 1L;

	// This helps us work around the "bug" where JComboBox eats the first Enter
	// press.
	private String lastSearchString;


	/**
	 * Creates a new <code>FindDialog</code>.
	 *
	 * @param owner The parent dialog.
	 * @param listener The component that listens for
	 *        {@link AbstractFindReplaceDialog#ACTION_FIND ACTION_FIND} actions.
	 */
	public FindDialog(Dialog owner, ActionListener listener) {
		super(owner);
		init(listener);
	}


	/**
	 * Creates a new <code>FindDialog</code>.
	 *
	 * @param owner The main window that owns this dialog.
	 * @param listener The component that listens for
	 *        {@link AbstractFindReplaceDialog#ACTION_FIND ACTION_FIND} actions.
	 */
	public FindDialog(Frame owner, ActionListener listener) {
		super(owner);
		init(listener);
	}


	/**
	 * Initializes find dialog-specific initialization stuff.
	 *
	 * @param listener The component that listens for
	 *        {@link AbstractFindReplaceDialog#ACTION_FIND ACTION_FIND} actions.
	 */
	private void init(ActionListener listener) {

		ComponentOrientation orientation = ComponentOrientation.
									getOrientation(getLocale());

		// Make a panel containing the "Find" edit box.
		JPanel enterTextPane = new JPanel(new SpringLayout());
		enterTextPane.setBorder(BorderFactory.createEmptyBorder(0,5,5,5));
		JTextComponent textField = getTextComponent(findTextCombo);
		textField.addFocusListener(new FindFocusAdapter());
		textField.addKeyListener(new FindKeyListener());
		textField.getDocument().addDocumentListener(new FindDocumentListener());
		JPanel temp = new JPanel(new BorderLayout());
		temp.add(findTextCombo);
		AssistanceIconPanel aip = new AssistanceIconPanel(findTextCombo);
		temp.add(aip, BorderLayout.LINE_START);
		if (orientation.isLeftToRight()) {
			enterTextPane.add(findFieldLabel);
			enterTextPane.add(temp);
		}
		else {
			enterTextPane.add(temp);
			enterTextPane.add(findFieldLabel);
		}

		UIUtil.makeSpringCompactGrid(enterTextPane, 1, 2,	//rows, cols
											0,0,		//initX, initY
											6, 6);	//xPad, yPad

		// Make a panel containing the inherited search direction radio
		// buttons and the inherited search options.
		JPanel bottomPanel = new JPanel(new BorderLayout());
		temp = new JPanel(new BorderLayout());
		bottomPanel.setBorder(UIUtil.getEmpty5Border());
		temp.add(searchConditionsPanel, BorderLayout.LINE_START);
		temp.add(dirPanel);
		bottomPanel.add(temp, BorderLayout.LINE_START);

		// Now, make a panel containing all the above stuff.
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(enterTextPane);
		leftPanel.add(bottomPanel);

		// Make a panel containing the action buttons.
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(2,1, 5,5));
		buttonPanel.add(findNextButton);
		buttonPanel.add(cancelButton);
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(buttonPanel, BorderLayout.NORTH);

		// Put everything into a neat little package.
		JPanel contentPane = new JPanel(new BorderLayout());
		if (orientation.isLeftToRight()) {
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,0,0,5));
		}
		else {
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,0,0));
		}
		contentPane.add(leftPanel);
		contentPane.add(rightPanel, BorderLayout.LINE_END);
		temp = new ResizableFrameContentPane(new BorderLayout());
		temp.add(contentPane, BorderLayout.NORTH);
		setContentPane(temp);
		getRootPane().setDefaultButton(findNextButton);
		setTitle(getString("FindDialogTitle"));
		setResizable(true);
		pack();
		setLocationRelativeTo(getParent());

		setSearchContext(new SearchDialogSearchContext());
		addActionListener(listener);

		applyComponentOrientation(orientation);

	}


	/**
	 * Overrides <code>JDialog</code>'s <code>setVisible</code> method; decides
	 * whether or not buttons are enabled.
	 *
	 * @param visible Whether or not the dialog should be visible.
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			String selectedItem = (String)findTextCombo.getSelectedItem();
			findNextButton.setEnabled(selectedItem!=null);
			super.setVisible(true);
			focusFindTextField();
		}
		else {
			super.setVisible(false);
		}
	}

	/**
	 * Called whenever the user changes the Look and Feel, etc.
	 * This is overridden so we can reinstate the listeners that are evidently
	 * lost on the JTextField portion of our combo box.
	 */
	public void updateUI() {
		JTextComponent textField = getTextComponent(findTextCombo);
		textField.addFocusListener(new FindFocusAdapter());
		textField.addKeyListener(new FindKeyListener());
		textField.getDocument().addDocumentListener(new FindDocumentListener());
	}


	/**
	 * Listens for changes in the text field (find search field).
	 */
	private class FindDocumentListener implements DocumentListener {

		public void insertUpdate(DocumentEvent e) {
			handleToggleButtons();
		}

		public void removeUpdate(DocumentEvent e) {
			JTextComponent comp = getTextComponent(findTextCombo);
			if (comp.getDocument().getLength()==0) {
				findNextButton.setEnabled(false);
			}
			else {
				handleToggleButtons();
			}
		}

		public void changedUpdate(DocumentEvent e) {
		}

	}


	/**
	 * Listens for the text field gaining focus.  All it does is select all
	 * text in the combo box's text area.
	 */
	private class FindFocusAdapter extends FocusAdapter {

		public void focusGained(FocusEvent e) {
			getTextComponent(findTextCombo).selectAll();
			// Remember what it originally was, in case they tabbed out.
			lastSearchString = (String)findTextCombo.getSelectedItem();
		}

	}


	/**
	 * Listens for key presses in the find dialog.
	 */
	private class FindKeyListener implements KeyListener {

		// Listens for the user pressing a key down.
		public void keyPressed(KeyEvent e) {
		}

		// Listens for a user releasing a key.
		public void keyReleased(KeyEvent e) {

			// This is an ugly hack to get around JComboBox's
			// insistence on eating the first Enter keypress
			// it receives when it has focus and its selected item
			// has changed since the last time it lost focus.
			if (e.getKeyCode()==KeyEvent.VK_ENTER && isPreJava6JRE()) {
				String searchString = (String)findTextCombo.getSelectedItem();
				if (!searchString.equals(lastSearchString)) {
					findNextButton.doClick(0);
					lastSearchString = searchString;
					getTextComponent(findTextCombo).selectAll();
				}
			}

		}

		// Listens for a key being typed.
		public void keyTyped(KeyEvent e) {
		}

	}


}