/**
 * Copyright (c) 2014 Netflix, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.msl.userauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.netflix.msl.MslCryptoException;
import com.netflix.msl.MslEncodingException;
import com.netflix.msl.MslError;
import com.netflix.msl.MslUserAuthException;
import com.netflix.msl.entityauth.EntityAuthenticationScheme;
import com.netflix.msl.test.ExpectedMslException;
import com.netflix.msl.tokens.MasterToken;
import com.netflix.msl.tokens.MockMslUser;
import com.netflix.msl.tokens.MslUser;
import com.netflix.msl.tokens.UserIdToken;
import com.netflix.msl.util.JsonUtils;
import com.netflix.msl.util.MockMslContext;
import com.netflix.msl.util.MslContext;
import com.netflix.msl.util.MslTestUtils;

/**
 * User ID token user authentication data unit tests.
 * 
 * @author Wesley Miaw <wmiaw@netflix.com>
 */
public class UserIdTokenAuthenticationDataTest {
    /** JSON key user authentication scheme. */
    private static final String KEY_SCHEME = "scheme";
    /** JSON key user authentication data. */
    private static final String KEY_AUTHDATA = "authdata";
    /** JSON master token key. */
    private static final String KEY_MASTER_TOKEN = "mastertoken";
    /** JSON user ID token key. */
    private static final String KEY_USER_ID_TOKEN = "useridtoken";
    
    @Rule
    public ExpectedMslException thrown = ExpectedMslException.none();
    
    /** Master token. */
    private static MasterToken MASTER_TOKEN;
    /** User ID token. */
    private static UserIdToken USER_ID_TOKEN;
    
    /** MSL context. */
    private static MslContext ctx;
    
    @BeforeClass
    public static void setup() throws MslEncodingException, MslCryptoException {
        ctx = new MockMslContext(EntityAuthenticationScheme.X509, false);
        MASTER_TOKEN = MslTestUtils.getMasterToken(ctx, 1L, 1L);
        final MslUser user = new MockMslUser(1);
        USER_ID_TOKEN = MslTestUtils.getUserIdToken(ctx, MASTER_TOKEN, 1L, user);
    }
    
    @AfterClass
    public static void teardown() {
        USER_ID_TOKEN = null;
        MASTER_TOKEN = null;
        ctx = null;
    }
    
    @Test
    public void ctors() throws MslEncodingException, MslUserAuthException {
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        assertEquals(UserAuthenticationScheme.USER_ID_TOKEN, data.getScheme());
        assertEquals(MASTER_TOKEN, data.getMasterToken());
        assertEquals(USER_ID_TOKEN, data.getUserIdToken());
        final JSONObject authdata = data.getAuthData();
        assertNotNull(authdata);
        final String jsonString = data.toJSONString();
        
        final UserIdTokenAuthenticationData joData = new UserIdTokenAuthenticationData(ctx, authdata);
        assertEquals(data.getScheme(), joData.getScheme());
        assertEquals(data.getMasterToken(), joData.getMasterToken());
        assertEquals(data.getUserIdToken(), joData.getUserIdToken());
        final JSONObject joAuthdata = joData.getAuthData();
        assertNotNull(joAuthdata);
        assertTrue(JsonUtils.equals(authdata, joAuthdata));
        final String joJsonString = joData.toJSONString();
        assertNotNull(joJsonString);
        assertEquals(jsonString, joJsonString);
    }
    
    @Test
    public void jsonString() {
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final JSONObject jo = new JSONObject(data.toJSONString());
        assertEquals(UserAuthenticationScheme.USER_ID_TOKEN.name(), jo.getString(KEY_SCHEME));
        final JSONObject authdata = jo.getJSONObject(KEY_AUTHDATA);
        final JSONObject masterTokenJo = authdata.getJSONObject(KEY_MASTER_TOKEN);
        assertTrue(JsonUtils.equals(new JSONObject(MASTER_TOKEN.toJSONString()), masterTokenJo));
        final JSONObject userIdTokenJo = authdata.getJSONObject(KEY_USER_ID_TOKEN);
        assertTrue(JsonUtils.equals(new JSONObject(USER_ID_TOKEN.toJSONString()), userIdTokenJo));
    }
    
    @Test
    public void create() throws MslUserAuthException, MslEncodingException, MslCryptoException {
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final String jsonString = data.toJSONString();
        final JSONObject jo = new JSONObject(jsonString);
        final UserAuthenticationData userdata = UserAuthenticationData.create(ctx, null, jo);
        assertNotNull(userdata);
        assertTrue(userdata instanceof UserIdTokenAuthenticationData);
        
        final UserIdTokenAuthenticationData joData = (UserIdTokenAuthenticationData)userdata;
        assertEquals(data.getScheme(), joData.getScheme());
        assertEquals(data.getMasterToken(), joData.getMasterToken());
        assertEquals(data.getUserIdToken(), joData.getUserIdToken());
        final JSONObject joAuthdata = joData.getAuthData();
        assertNotNull(joAuthdata);
        assertTrue(JsonUtils.equals(data.getAuthData(), joAuthdata));
        final String joJsonString = joData.toJSONString();
        assertNotNull(joJsonString);
        assertEquals(jsonString, joJsonString);
        
    }
    
    @Test
    public void missingMasterToken() throws MslEncodingException, MslUserAuthException {
        thrown.expect(MslEncodingException.class);
        thrown.expectMslError(MslError.JSON_PARSE_ERROR);
        
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final JSONObject authdata = data.getAuthData();
        authdata.remove(KEY_MASTER_TOKEN);
        new UserIdTokenAuthenticationData(ctx, authdata);
    }
    
    @Test
    public void invalidMasterToken() throws MslEncodingException, MslUserAuthException {
        thrown.expect(MslUserAuthException.class);
        thrown.expectMslError(MslError.USERAUTH_MASTERTOKEN_INVALID);
        
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final JSONObject authdata = data.getAuthData();
        authdata.put(KEY_MASTER_TOKEN, new JSONObject());
        new UserIdTokenAuthenticationData(ctx, authdata);
    }
    
    @Test
    public void missingUserIdToken() throws MslEncodingException, MslUserAuthException {
        thrown.expect(MslEncodingException.class);
        thrown.expectMslError(MslError.JSON_PARSE_ERROR);
        
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final JSONObject authdata = data.getAuthData();
        authdata.remove(KEY_USER_ID_TOKEN);
        new UserIdTokenAuthenticationData(ctx, authdata);
    }
    
    @Test
    public void invalidUserIdToken() throws MslEncodingException, MslUserAuthException {
        thrown.expect(MslUserAuthException.class);
        thrown.expectMslError(MslError.USERAUTH_USERIDTOKEN_INVALID);
        
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final JSONObject authdata = data.getAuthData();
        authdata.put(KEY_USER_ID_TOKEN, new JSONObject());
        new UserIdTokenAuthenticationData(ctx, authdata);
    }
    
    @Test
    public void mismatchedTokens() throws MslEncodingException, MslCryptoException, MslUserAuthException {
        thrown.expect(MslUserAuthException.class);
        thrown.expectMslError(MslError.USERAUTH_USERIDTOKEN_INVALID);
        
        final MasterToken masterToken = MslTestUtils.getMasterToken(ctx, MASTER_TOKEN.getSequenceNumber(), MASTER_TOKEN.getSerialNumber() + 1);
        
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final JSONObject authdata = data.getAuthData();
        authdata.put(KEY_MASTER_TOKEN, new JSONObject(masterToken.toJSONString()));
        new UserIdTokenAuthenticationData(ctx, authdata);
    }
    
    @Test
    public void equalsMasterToken() throws MslEncodingException, MslCryptoException, MslUserAuthException {
        final MasterToken masterToken = MslTestUtils.getMasterToken(ctx, MASTER_TOKEN.getSequenceNumber() + 1, MASTER_TOKEN.getSerialNumber());
        
        final UserIdTokenAuthenticationData dataA = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final UserIdTokenAuthenticationData dataB = new UserIdTokenAuthenticationData(masterToken, USER_ID_TOKEN);
        final UserIdTokenAuthenticationData dataA2 = new UserIdTokenAuthenticationData(ctx, dataA.getAuthData());
        
        assertTrue(dataA.equals(dataA));
        assertEquals(dataA.hashCode(), dataA.hashCode());
        
        assertFalse(dataA.equals(dataB));
        assertFalse(dataB.equals(dataA));
        assertTrue(dataA.hashCode() != dataB.hashCode());
        
        assertTrue(dataA.equals(dataA2));
        assertTrue(dataA2.equals(dataA));
        assertEquals(dataA.hashCode(), dataA2.hashCode());
    }
    
    @Test
    public void equalsUserIdToken() throws MslEncodingException, MslCryptoException, MslUserAuthException {
        final UserIdToken userIdToken = MslTestUtils.getUserIdToken(ctx, MASTER_TOKEN, USER_ID_TOKEN.getSerialNumber() + 1, USER_ID_TOKEN.getUser());
        
        final UserIdTokenAuthenticationData dataA = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        final UserIdTokenAuthenticationData dataB = new UserIdTokenAuthenticationData(MASTER_TOKEN, userIdToken);
        final UserIdTokenAuthenticationData dataA2 = new UserIdTokenAuthenticationData(ctx, dataA.getAuthData());
        
        assertTrue(dataA.equals(dataA));
        assertEquals(dataA.hashCode(), dataA.hashCode());
        
        assertFalse(dataA.equals(dataB));
        assertFalse(dataB.equals(dataA));
        assertTrue(dataA.hashCode() != dataB.hashCode());
        
        assertTrue(dataA.equals(dataA2));
        assertTrue(dataA2.equals(dataA));
        assertEquals(dataA.hashCode(), dataA2.hashCode());
    }
    
    @Test
    public void equalsObject() {
        final UserIdTokenAuthenticationData data = new UserIdTokenAuthenticationData(MASTER_TOKEN, USER_ID_TOKEN);
        assertFalse(data.equals(null));
        assertFalse(data.equals(KEY_MASTER_TOKEN));
        assertTrue(data.hashCode() != KEY_MASTER_TOKEN.hashCode());
    }
}
