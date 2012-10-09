/*******************************************************************************
 * Copyright (c) 2012 Thales Global Services SAS                                *
 * Author : Aravindan Mahendran                                                 *
 *                                                                              *
 * The MIT License                                                              *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/

package com.thalesgroup.hudson.plugins.gnat.migration;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.thalesgroup.hudson.plugins.gnat.gnatcheck.GnatcheckBuilder;
import com.thalesgroup.hudson.plugins.gnat.gnatcheck.GnatcheckPublisher;
import com.thalesgroup.hudson.plugins.gnat.gnatmetric.GnatmetricBuilder;
import com.thalesgroup.hudson.plugins.gnat.gnatmetric.GnatmetricPublisher;

@Extension
public class GnatCheckMigrationListener extends ItemListener {
	private static Logger LOGGER = Logger.getLogger(GnatCheckMigrationListener.class.getName());


	@Override
	public void onLoaded() {
		List<TopLevelItem> topLevelItems = Hudson.getInstance().getItems();
		for (TopLevelItem topLevelItem : topLevelItems){
			try {
				if (topLevelItem instanceof Project){

					Project project = (Project) topLevelItem;
					DescribableList<Publisher, Descriptor<Publisher>> publishersList = project.getPublishersList();
					List<Builder> builders = project.getBuilders();
					Iterator<Publisher> iterator = publishersList.iterator();

					while (iterator.hasNext()){
						Publisher publisher = iterator.next();
						
						if (publisher instanceof GnatcheckPublisher){
							GnatcheckBuilder gnatCheckBuilder = new GnatcheckBuilder(((GnatcheckPublisher) publisher).types);
							DescribableList<Builder, Descriptor<Builder>> buildersList = new DescribableList<Builder, Descriptor<Builder>>(topLevelItem, builders);
							Field buildersField = Project.class.getDeclaredField("builders");
							buildersField.setAccessible(true);
							buildersList.add(gnatCheckBuilder);
							buildersField.set(project, buildersList);
							iterator.remove();
							topLevelItem.save();
						}
						
						
					}
				}
			} catch (SecurityException e) {
				LOGGER.log(Level.SEVERE, "Migration of Gnat publishers failed because of the item %s", topLevelItem.getName());
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				LOGGER.log(Level.SEVERE, "Migration of Gnat publishers failed because of the item %s", topLevelItem.getName());
				e.printStackTrace();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Migration of Gnat publishers failed because of the item %s", topLevelItem.getName());
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				LOGGER.log(Level.SEVERE, "Migration of Gnat publishers failed because of the item %s", topLevelItem.getName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				LOGGER.log(Level.SEVERE, "Migration of Gnat publishers failed because of the item %s", topLevelItem.getName());
				e.printStackTrace();
			}
		}
	}

}
