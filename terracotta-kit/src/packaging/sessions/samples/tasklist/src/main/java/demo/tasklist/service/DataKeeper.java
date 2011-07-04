/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.tasklist.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DataKeeper keeps track of the current state of the task list. All
 * modifications to the task list are made by calling DataKeeper's methods.
 */
public class DataKeeper implements java.io.Serializable {
	private static final long serialVersionUID = 5664956817748843899L;
	private final List<String> userList;

	public DataKeeper() {
		userList = new ArrayList<String>();
	}

	public void addListItem(String newListItem) {
		if (newListItem != null) {
			userList.add(newListItem);
		}
	}

	public void deleteListItems(String[] itemsForDelete) {
		if (itemsForDelete != null) {
			for (int i = 0; i < itemsForDelete.length; i++) {
				userList.remove(itemsForDelete[i]);
			}
		}
	}

	public int getListSize() {
		if (userList == null) {
			return 0;
		}
		return userList.size();
	}

	public String getListItem(int index) {
		return userList.get(index);
	}

	public List<String> getList() {
		return userList;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean startedSeps = false;
		for (Iterator<String> iter = getList().iterator(); iter.hasNext();) {
			if (startedSeps) {
				sb.append(", ");
			} else {
				startedSeps = true;
			}
			sb.append(iter.next());
		}
		return sb.toString();
	}
}