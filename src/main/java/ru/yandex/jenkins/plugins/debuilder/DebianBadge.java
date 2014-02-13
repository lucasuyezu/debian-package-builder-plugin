package ru.yandex.jenkins.plugins.debuilder;

import java.text.MessageFormat;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.BuildBadgeAction;

@ExportedBean
public class DebianBadge implements BuildBadgeAction {
    private String module = "";
    private String version = "";
    private String repository = "";
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

    @Exported
    public String getModule() {
        return module;
    }

    @Exported
    public String getVersion() {
        return version;
    }

    @Exported
    public String getRepository() {
        return repository;
    }

    @Exported
    public void setRepository(String repositoryName) {
        repository = repositoryName;
    }
}
