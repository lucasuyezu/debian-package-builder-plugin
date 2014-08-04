package ru.yandex.jenkins.plugins.debuilder;

import java.text.MessageFormat;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.BuildBadgeAction;

@ExportedBean
public class DebianBadge implements BuildBadgeAction {
	/**
	 * The full path to the debian directory
	 */
	private String module = "";
	
	/**
	 * The path to the debian directory relative to workspace
	 */
	private String relativeModule = "";
	
	/**
	 * The version of this badge 
	 */
	private String version = "";
	
	/**
	 * The name of the package
	 */
	private String sourceName = "";
	
	private String text = "built deb";
	private String color = "#000000";
	private String background = "#FFDA47";
	private String border = "1px";
	private String borderColor = "#0066FF";

	public DebianBadge(String latestVersion, String module) {
		this.version = latestVersion;
		this.text = MessageFormat.format("deb {0}", latestVersion);
		this.module = module;
	}
	
	public DebianBadge(String sourceName, String latestVersion, String module, String relativeModule) {
		this.version = latestVersion;
		this.text = MessageFormat.format("deb {0}", latestVersion);
		this.module = module;
		this.relativeModule = relativeModule;
		this.sourceName = sourceName;
	}

	@Override
	public String getIconFileName() {
		return "";
	}

	@Override
	public String getDisplayName() {
		return "";
	}

	@Override
	public String getUrlName() {
		return null;
	}

	@Exported
	public String getText() {
		return text;
	}

	@Exported
	public String getColor() {
		return color;
	}

	@Exported
	public String getBackground() {
		return background;
	}

	@Exported
	public String getBorder() {
		return border;
	}

	@Exported
	public String getBorderColor() {
		return borderColor;
	}

	/**
	 * Get the {@link #module}
	 *
	 * @return {@link #module}
	 */
	@Exported
	public String getModule() {
		return module;
	}
	
	/**
	 * Get the {@link #relativeModule}
	 *
	 * @return {@link #relativeModule}
	 */
	@Exported
	public String getRelativeModule() {
	    return relativeModule;
	}

    @Exported
    public String getVersion() {
        return version;
    }
    
    /**
	 * Get the {@link #sourceName}
	 *
	 * @return {@link #sourceName}
	 */
	@Exported
	public String getSourceName() {
	    return sourceName;
	}
}
