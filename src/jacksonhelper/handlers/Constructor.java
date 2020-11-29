package jacksonhelper.handlers;

public class Constructor {

	private final String constructorHeader;
	private final String constructorBody;
	private final int constructorStartLine;
	private final int constructorEndLine;

	public Constructor(String constructorHeader, String constructorBody, int constructorStartLine, int constructorEndLine) {
		this.constructorHeader = constructorHeader;
		this.constructorBody = constructorBody;
		this.constructorStartLine = constructorStartLine;
		this.constructorEndLine = constructorEndLine;
	}

	public String getHeader() {
		return constructorHeader;
	}

	public String getBody() {
		return constructorBody;
	}

	public int getStartLine() {
		return constructorStartLine;
	}

	public int getEndLine() {
		return constructorEndLine;
	}

}
