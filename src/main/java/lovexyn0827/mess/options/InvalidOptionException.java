package lovexyn0827.mess.options;

import lovexyn0827.mess.util.i18n.I18N;

// Couldn't extend TranslatableException because it shouldn't be a RuntimeException
public class InvalidOptionException extends Exception {
	private Object[] args;

	public InvalidOptionException(String string, Object ... args) {
		super(string);
		this.args = args;
	}
	
	public String getMessageWithoutArgs() {
		if(this.args != null && this.args.length != 0) {
			throw new IllegalStateException("Some args are given, use getMessage() instead!");
		}
		
		return I18N.translate(super.getMessage());
	}
	
	@Override
	public String getMessage() {
		return String.format(I18N.translate(super.getMessage()), args);
	}

	private static final long serialVersionUID = 1L;

}
