package nl.dobots.bluenetexample;

/**
 * Copyright (c) 2015 Bart van Vliet <bart@dobots.nl>. All rights reserved.
 * <p/>
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3, as
 * published by the Free Software Foundation.
 * <p/>
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * <p/>
 * Created on 30-11-16
 *
 * @author Bart van Vliet
 */
public class Config {
	public static final boolean ENCRYPTION_ENABLED = true;
	public static final String ADMIN_KEY  = "adminKeyForCrown";
	public static final String MEMBER_KEY = "memberKeyForHome";
	public static final String GUEST_KEY  = "guestKeyForGirls";
	public static final int    MESH_ACCESS_ADDRESS = 12324;
	public static final String IBEACON_UUID = "b643423e-e175-4af0-a2e4-31e32f729a8a";
	public static final int    IBEACON_MAJOR = 123;
	public static final int    IBEACON_MINOR = 456;
}
