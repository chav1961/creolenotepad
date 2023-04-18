package chav1961.creolenotepad;

public enum PreviewMode {
	EDIT("editor"),
	VIEW("viewer");
	
	private final String	cardName;
	
	private PreviewMode(final String cardName) {
		this.cardName = cardName;
	}
	
	public String getCardName() {
		return cardName;
	}
}
