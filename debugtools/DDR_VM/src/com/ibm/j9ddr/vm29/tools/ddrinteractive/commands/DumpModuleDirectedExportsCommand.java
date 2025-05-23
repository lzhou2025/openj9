/*
 * Copyright IBM Corp. and others 2018
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 */
package com.ibm.j9ddr.vm29.tools.ddrinteractive.commands;

import java.io.PrintStream;

import com.ibm.j9ddr.CorruptDataException;
import com.ibm.j9ddr.tools.ddrinteractive.Command;
import com.ibm.j9ddr.tools.ddrinteractive.CommandUtils;
import com.ibm.j9ddr.tools.ddrinteractive.Context;
import com.ibm.j9ddr.tools.ddrinteractive.DDRInteractiveCommandException;
import com.ibm.j9ddr.vm29.j9.DataType;
import com.ibm.j9ddr.vm29.j9.HashTable;
import com.ibm.j9ddr.vm29.j9.ModuleHashTable;
import com.ibm.j9ddr.vm29.j9.SlotIterator;
import com.ibm.j9ddr.vm29.pointer.generated.J9HashTablePointer;
import com.ibm.j9ddr.vm29.pointer.generated.J9JavaVMPointer;
import com.ibm.j9ddr.vm29.pointer.generated.J9ModulePointer;
import com.ibm.j9ddr.vm29.pointer.generated.J9PackagePointer;
import com.ibm.j9ddr.vm29.pointer.helper.J9RASHelper;
import com.ibm.j9ddr.vm29.tools.ddrinteractive.JavaVersionHelper;
import com.ibm.j9ddr.vm29.tools.ddrinteractive.ModularityHelper;

/**
 * DumpModuleDirectedExports command dumps all modules that the package is exported to
 * 
 * Example:
 *    !dumpmoduledirectedexports 0x00000130550DA7C8 
 * Example output: 
 *    jdk.jartool    !j9module 0x000001305F46B6E8
 */
public class DumpModuleDirectedExportsCommand extends Command
{

	public DumpModuleDirectedExportsCommand()
	{
		addCommand("dumpmoduledirectedexports", "<packageAddress>", "dump all modules that the package is exported to");
	}
	
	public void run(String command, String[] args, Context context, PrintStream out) throws DDRInteractiveCommandException 
	{
		if (args.length != 1) {
			CommandUtils.dbgPrint(out, "Usage: !dumpmoduledirectedexports <packageAddress>\n");
			return;
		}
		try {
			J9JavaVMPointer vm = J9RASHelper.getVM(DataType.getJ9RASPointer());
			int hitCount = 0;
			if (JavaVersionHelper.ensureJava9AndUp(vm, out)) {
				String targetPackageAddress = args[0];
				J9PackagePointer packagePtr = J9PackagePointer.cast(Long.decode(targetPackageAddress));
				J9HashTablePointer exportTable = packagePtr.exportsHashTable();
				HashTable<J9ModulePointer> moduleHashTable = ModuleHashTable.fromJ9HashTable(exportTable);
				SlotIterator<J9ModulePointer> slotIterator = moduleHashTable.iterator();
				while (slotIterator.hasNext()) {
					J9ModulePointer exportModulePtr = slotIterator.next();
					hitCount++;
					String moduleName = ModularityHelper.getModuleName(exportModulePtr);
					String hexAddress = exportModulePtr.getHexAddress();
					out.printf("%-30s !j9module %s%n", moduleName, hexAddress);
				}
				out.println(String.format("Found %d module(s) that the package is exported to\n", hitCount));
			}
		} catch (CorruptDataException e) {
			throw new DDRInteractiveCommandException(e);
		}
	}
}

