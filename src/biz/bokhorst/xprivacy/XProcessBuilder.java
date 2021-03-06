package biz.bokhorst.xprivacy;

import java.io.IOException;
import java.util.List;

import android.text.TextUtils;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XProcessBuilder extends XHook {

	private String mCommand;

	public XProcessBuilder(String methodName, String restrictionName, String[] permissions, String command) {
		super(methodName, restrictionName, permissions, command);
		mCommand = command;
	}

	// public Process start()
	// libcore/luni/src/main/java/java/lang/ProcessBuilder.java

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		// Get commands
		ProcessBuilder builder = (ProcessBuilder) param.thisObject;
		List<String> listProg = builder.command();

		// Check commands
		String command = TextUtils.join(" ", listProg);
		XUtil.log(this, Log.INFO, "start(" + command + ")");
		if ((mCommand == null && !command.startsWith("sh") && !command.startsWith("su"))
				|| (mCommand != null && command.startsWith(mCommand)))
			if (isRestricted(param, mCommand == null ? getMethodName() : mCommand))
				param.setThrowable(new IOException(XRestriction.getDefacedString()));
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		// Do nothing
	}
}
