/*
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of the LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.commander.operations;

import java.nio.file.*;

import org.luwrain.core.NullCheck;

public class Operations
{
    static public Operation copy(Listener listener, String opName,
				 Path[] copyFrom, Path copyTo)
    {
	return new Copy(listener, opName, copyFrom, copyTo);
    }

    static public Operation move(Listener listener, String name,
				 Path[] toMove, Path moveTo)
    {
	NullCheck.notNull(name, "name");
	NullCheck.notNullItems(toMove, "toMove");
	NullCheck.notEmptyArray(toMove, "toMove");
	NullCheck.notNull(moveTo, "moveTo");
	return new Move(listener, name, toMove, moveTo);
    }

    static public Operation delete(Listener listener, String opName,
				 Path[] deleteWhat)
    {
	return new Delete(listener, opName, deleteWhat);
    }
}
