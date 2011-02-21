/**
 * This file is part of Maven Deployit plugin.
 *
 * Maven Deployit plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maven Deployit plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Maven Deployit plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.jar.Attributes
import java.util.jar.Manifest

def void checkIsFile(file) {
	assert file.exists(), " file " + file;
	assert file.isFile(), " file " + file;
}

def checkIsDir(File file) {
	assert file.exists(), " file " + file;
	assert file.isDirectory(), " file " + file;
}

def assertEntry(entries, entry, attribute, expectedvalue) {
	def configEntries = entries.findAll { key, value -> key == entry};
	assert configEntries.size() == 1, entry + " not found";
	def earEntry = configEntries.iterator().next();
	def attributes = earEntry.getValue();
	assert attributes.containsKey(new Attributes.Name(attribute)), "does not contain " + attribute;
	assert attributes.get(new Attributes.Name(attribute)).equals(expectedvalue), "does not equals to" + expectedvalue;
}

def darDirectory = new File(basedir, "target/deployment-package/dar-format/1.0");
checkIsDir(darDirectory);
checkIsDir(new File(darDirectory, "Ear"));
checkIsDir(new File(darDirectory, "SqlFiles"));
checkIsDir(new File(darDirectory, "ConfigurationFiles"));
checkIsFile(new File(darDirectory, "META-INF/MANIFEST.MF"));

def manifest = new File(darDirectory, "META-INF/MANIFEST.MF");
manifest.eachLine {line ->
	println line
}

def m = new Manifest(manifest.newInputStream())

def entries = m.getEntries();
assert entries.size() == 6, "6 entries";

assertEntry(entries,"ConfigurationFiles/resources.dir", "CI-Type", "ConfigurationFiles");
assertEntry(entries,"Ear/PetClinic-1.0.ear", "CI-Type", "Ear");
assertEntry(entries,"Ear/PetClinic-1.0.ear", "CI-Name", "PetClinic");
assertEntry(entries,"petclinicDSResource", "CI-Type", "DummyDataSource");
assertEntry(entries,"petclinicDSResource", "CI-driver", "com.mysql.jdbc.Driver");
assertEntry(entries,"petclinicDSResource", "CI-settings-EntryKey-1", "autoCommit");
assertEntry(entries,"petclinicDSResource", "CI-settings-EntryValue-1", "true");


assertEntry(entries,"SqlScript/afile.sql", "CI-Type", "SqlScript");


return true;
