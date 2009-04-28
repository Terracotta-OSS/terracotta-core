package com.tc.admin.options;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XSpinner;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.DefaultEditor;

public class RuntimeStatsOption extends AbstractOption {
  public static final String  NAME                                    = "RuntimeStats";

  private SpinnerNumberModel  samplePeriodModel;
  private SpinnerNumberModel  sampleTimeoutModel;
  private SpinnerNumberModel  sampleHistoryModel;
  private ImageIcon           icon;

  private static final int    DEFAULT_POLL_PERIOD_SECS                = 3;
  private static final int    DEFAULT_POLL_TIMEOUT_SECS               = 1;
  private static final int    DEFAULT_SAMPLE_HISTORY_MINUTES          = 5;
  private static final int    SAMPLE_SAMPLE_HISTORY_STEP_SIZE         = 1;

  private static final String DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY    = "poll-periods-seconds";
  private static final String DEFAULT_POLL_TIMEOUT_SECONDS_PREF_KEY   = "poll-timeout-seconds";
  private static final String DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY = "sample-history-minutes";

  public RuntimeStatsOption(ApplicationContext appContext) {
    super(appContext, NAME);
  }

  @Override
  public String getLabel() {
    return "Runtime stats";
  }

  @Override
  public Icon getIcon() {
    if (icon == null) {
      icon = new ImageIcon(getClass().getResource("/com/tc/admin/icons/chart_bar.png"));
    }
    return icon;
  }

  @Override
  public void apply() {
    putIntPref(DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY, samplePeriodModel.getNumber().intValue());
    putIntPref(DEFAULT_POLL_TIMEOUT_SECONDS_PREF_KEY, sampleTimeoutModel.getNumber().intValue());
    putIntPref(DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY, sampleHistoryModel.getNumber().intValue());
  }

  @Override
  public Component getDisplay() {
    if (display == null) {
      XContainer panel = new XContainer(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = gbc.gridy = 0;
      gbc.insets = new Insets(3, 3, 3, 3);
      gbc.anchor = GridBagConstraints.EAST;

      panel.add(new XLabel("Poll period seconds:"), gbc);
      gbc.gridx++;
      gbc.anchor = GridBagConstraints.WEST;

      XSpinner samplePeriodSpinner = new XSpinner();
      samplePeriodSpinner.setToolTipText("Sample poll period (seconds)");
      samplePeriodModel = new SpinnerNumberModel(Integer.valueOf(getPollPeriodSeconds()), Integer.valueOf(1), null,
                                                 Integer.valueOf(1));
      samplePeriodSpinner.setModel(samplePeriodModel);
      ((DefaultEditor) samplePeriodSpinner.getEditor()).getTextField().setColumns(3);
      panel.add(samplePeriodSpinner, gbc);
      gbc.gridx--;
      gbc.gridy++;

      panel.add(new XLabel("Poll timeout seconds:"), gbc);
      gbc.gridx++;
      gbc.anchor = GridBagConstraints.WEST;

      XSpinner sampleTimeoutSpinner = new XSpinner();
      samplePeriodSpinner.setToolTipText("Sample poll timeout (seconds)");
      sampleTimeoutModel = new SpinnerNumberModel(Integer.valueOf(getPollTimeoutSeconds()), Integer.valueOf(1), null,
                                                  Integer.valueOf(1));
      sampleTimeoutSpinner.setModel(sampleTimeoutModel);
      ((DefaultEditor) sampleTimeoutSpinner.getEditor()).getTextField().setColumns(3);
      panel.add(sampleTimeoutSpinner, gbc);
      gbc.gridx--;
      gbc.gridy++;

      gbc.anchor = GridBagConstraints.EAST;
      panel.add(new XLabel("History minutes:"), gbc);
      gbc.gridx++;
      gbc.anchor = GridBagConstraints.WEST;

      XSpinner sampleHistorySpinner = new XSpinner();
      sampleHistorySpinner.setToolTipText("Number of minutes of samples to display");
      sampleHistoryModel = new SpinnerNumberModel(Integer.valueOf(getSampleHistoryMinutes()), Integer.valueOf(1), null,
                                                  Integer.valueOf(SAMPLE_SAMPLE_HISTORY_STEP_SIZE));
      sampleHistorySpinner.setModel(sampleHistoryModel);
      ((DefaultEditor) sampleHistorySpinner.getEditor()).getTextField().setColumns(3);
      panel.add(sampleHistorySpinner, gbc);
      panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
      display = panel;
      display.setName(getName());
    } else {
      samplePeriodModel.setValue(getPollPeriodSeconds());
      sampleHistoryModel.setValue(getSampleHistoryMinutes());
    }

    return display;
  }

  public int getPollPeriodSeconds() {
    return getIntPref(DEFAULT_POLL_PERIOD_SECONDS_PREF_KEY, DEFAULT_POLL_PERIOD_SECS);
  }

  public int getPollTimeoutSeconds() {
    return getIntPref(DEFAULT_POLL_TIMEOUT_SECONDS_PREF_KEY, DEFAULT_POLL_TIMEOUT_SECS);
  }

  public int getSampleHistoryMinutes() {
    return getIntPref(DEFAULT_SAMPLE_HISTORY_MINUTES_PREF_KEY, DEFAULT_SAMPLE_HISTORY_MINUTES);
  }
}
