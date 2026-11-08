package uk.co.bithatch.macrolib;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.List;

/**
 * The Class CommandMacro.
 */
public class CommandMacro extends Macro {
	final static Logger LOG = System.getLogger(CommandMacro.class.getName());

	private String[] arguments;
	private String command;

	{
		setType(TargetType.COMMAND);
	}

	/**
	 * Instantiates a new command macro.
	 */
	public CommandMacro() {
	}

	/**
	 * Instantiates a new command macro.
	 *
	 * @param activatedBy the activated by
	 * @param command     the command
	 * @param arguments   the arguments
	 */
	public CommandMacro(KeySequence activatedBy, String command, String... arguments) {
		super(activatedBy);
		setCommand(command);
		setArguments(arguments);
	}

	public CommandMacro(Macro source) {
		super(source);
		if (source instanceof CommandMacro) {
			this.command = ((CommandMacro) source).command;
			this.arguments = ((CommandMacro) source).arguments;
		}
	}

	/**
	 * Clone.
	 *
	 * @return the macro
	 * @throws CloneNotSupportedException the clone not supported exception
	 */
	@Override
	public Macro clone() throws CloneNotSupportedException {
		return new CommandMacro(this);
	}

	/**
	 * Do macro.
	 *
	 * @param execution the execution
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	@Override
	public boolean doMacro(MacroExecution execution) throws Exception {

		LOG.log(Level.WARNING, String.format("Running external command '%s'", command));
		ProcessBuilder b = new ProcessBuilder();
		b.command().add(command);
		if (arguments != null)
			b.command().addAll(Arrays.asList(arguments));
		b.redirectErrorStream(true);
		Process p = b.start();
		p.getInputStream().transferTo(System.out);
		try {
			if (p.waitFor() != 0)
				LOG.log(Level.WARNING,
						String.format("External command '%s' failed with exit status %d", command, p.exitValue()));
		} catch (InterruptedException ie) {
			LOG.log(Level.WARNING, String.format("External command '%s' interrupted.", command));
		}
		return true;
	}

	/**
	 * Gets the arguments.
	 *
	 * @return the arguments
	 */
	public String[] getArguments() {
		return arguments;
	}

	/**
	 * Gets the command.
	 *
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Sets the arguments.
	 *
	 * @param arguments the new arguments
	 */
	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * Sets the arguments.
	 *
	 * @param arguments the new arguments
	 */
	public void setArguments(List<String> arguments) {
		this.arguments = arguments.toArray(new String[0]);
	}

	/**
	 * Sets the command.
	 *
	 * @param command the new command
	 */
	public void setCommand(String command) {
		this.command = command;
	}

}
