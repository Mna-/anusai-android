package com.agorikov.rsdnhome.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.agorikov.rsdnhome.common.Builder;

public final class PreferencesData {

	private final List<Forum> selectedForums;

	private PreferencesData(final List<Forum> selectedForums) {
		this.selectedForums = selectedForums;
	}

	public List<Forum> getSelectedForums() {
		return selectedForums;
	}
	
	
	public static class PreferencesDataBuilder implements Builder<PreferencesData> {

		private List<Forum> selectedForums;
		
		public PreferencesDataBuilder selectedForums(final List<Forum> selectedForums) {
			this.selectedForums = Collections.unmodifiableList(new ArrayList<Forum>(selectedForums));
			return this;
		}
		
		@Override
		public PreferencesData build() {
			return new PreferencesData(selectedForums);
		}
	}
	
	
	
	
}
