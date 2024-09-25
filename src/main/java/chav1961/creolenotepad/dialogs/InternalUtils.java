package chav1961.creolenotepad.dialogs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.JTextComponent;

class InternalUtils {
	public static boolean find(final JTextComponent editor, final String toFind, final boolean backward, final boolean wholeWord, final boolean useRegex) {
		final int		pos = editor.getCaretPosition();
		final String 	content = (backward ? editor.getText().substring(0, pos) : editor.getText().substring(pos)).replace("\r", "");
		final String	expr = useRegex ? toFind : "\\Q"+toFind+"\\E";
		final String	wordExpr = wholeWord ? "(\\s+|^)" + expr +"(\\s+|$)" : expr;
		final Pattern	p = Pattern.compile(wordExpr, Pattern.DOTALL);
		final Matcher	m = p.matcher(content);
		
		if (backward) {
			int	from = -1, to = 0;
			
			while (to < content.length() && m.find(to)) {
				if (from >= 0 && m.end() == pos) {
					break;
				}
				else {
					from = m.start();
					to = m.end();
				}
			}
			if (from >= 0) {
				editor.setSelectionStart(from);
				editor.setSelectionEnd(to);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			if (m.find()) {
				editor.setSelectionStart(pos + m.start());
				editor.setSelectionEnd(pos + m.end());
				return true;
			}
			else {
				return false;
			}
		}
	}
}
