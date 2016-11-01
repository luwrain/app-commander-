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

package org.luwrain.app.commander;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.app.commander.operations.Operation;

class OperationsAppearance implements ListArea.Appearance
{
    private Luwrain luwrain;
    private Strings strings;
    private Base base;

    OperationsAppearance(Luwrain luwrain, Strings strings,
Base base)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(strings, "strings");
	NullCheck.notNull(base, "base");
	this.luwrain = luwrain;
	this.strings = strings;
	this.base = base;
    }

    @Override public void announceItem(Object item, Set<Flags> flags)
    {
	NullCheck.notNull(item, "item");
	NullCheck.notNull(flags, "flags");

	final Operation op = (Operation)item;
	if (op.isFinished())
	{
	    luwrain.say(base.opResultDescr(op) + " " + op.getOperationName());
	    return;
	}
	final int percents = op.getPercents();
	if (percents > 0)
	    luwrain.say("" + luwrain.i18n().getNumberStr(percents, "percents") + " " + op.getOperationName()); else
	    luwrain.say(op.getOperationName());


    }

    @Override public String getScreenAppearance(Object item, Set<Flags> flags)
    {
	NullCheck.notNull(item, "item");
	NullCheck.notNull(flags, "flags");
	final Operation op = (Operation)item;
	if (op.isFinished())
	    return base.opResultDescr(op);
	final int percents = op.getPercents();
	if (percents == 0)
	    return op.getOperationName() + "...";
	return  percents + "%: "+ op.getOperationName();
    }

    @Override public int getObservableLeftBound(Object item)
    {
	return 0;
    }

    @Override public int getObservableRightBound(Object item)
    {
	return getScreenAppearance(item, EnumSet.noneOf(Flags.class)).length();
    }
}