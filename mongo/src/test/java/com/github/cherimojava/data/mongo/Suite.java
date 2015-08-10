/**
 * Copyright (C) 2013 cherimojava (http://github.com/cherimojava/cherimodata) Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.cherimojava.data.mongo;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.dynamicsuite.ClassPath;
import org.junit.extensions.dynamicsuite.Filter;
import org.junit.extensions.dynamicsuite.TestClassFilter;
import org.junit.extensions.dynamicsuite.suite.DynamicSuite;
import org.junit.runner.RunWith;

import com.google.common.base.Throwables;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.config.Timeout;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.extract.UserTempNaming;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.Downloader;

@RunWith( DynamicSuite.class )
@ClassPath
@Filter( Suite.class )
public class Suite
    implements TestClassFilter
{

    protected static final String storagePath = "/tmp/" + UUID.randomUUID();

    private static int port = 0;

    private static MongodExecutable exe = null;

    private static boolean suiteStarted = true;

    private static final boolean useRunningMongo = true;

    private static boolean running = false;

    private static Version version()
    {
        return Version.V2_6_1;
    }

    private static IMongodConfig mongodConfig()
        throws IOException
    {
        return new MongodConfigBuilder().version( version() ).net( new Net( getPort(), Network.localhostIsIPv6() ) )
            .replication( new Storage( storagePath, null, 0 ) ).timeout( new Timeout() ).build();
    }

    private static IRuntimeConfig runtimeConfig()
    {
        FixedPath path = new FixedPath( "bin/" );
        return new RuntimeConfigBuilder().defaults( Command.MongoD )
            .artifactStore( new ArtifactStoreBuilder().downloader( new Downloader() )
                .executableNaming( new UserTempNaming() ).tempDir( path ).download(
                    new DownloadConfigBuilder().defaultsForCommand( Command.MongoD ).artifactStorePath( path ) ) )
            .build();
    }

    private static MongodExecutable mongodExecutable()
        throws IOException
    {
        MongodStarter runtime = MongodStarter.getInstance( runtimeConfig() );
        return runtime.prepare( mongodConfig() );
    }

    @Override
    public boolean include( String className )
    {
        String[] parts = className.split( "\\." );
        return parts.length != 0 && parts[parts.length - 1].startsWith( "_" );
    }

    @Override
    public boolean include( @SuppressWarnings( "rawtypes" ) Class clazz)
    {
        return ( TestBase.class.isAssignableFrom( clazz ) );
    }

    public static synchronized int getPort()
    {
        if ( port == 0 )
        {
            if ( useRunningMongo )
            {
                // try if we can connect to a running MongoDB process
                try
                {
                    new Socket( "127.0.0.1", 27017 );
                    port = 27017;
                    return port;
                }
                catch ( Exception e )
                {
                    // once with logging log
                    e.printStackTrace();
                }
            }
            // try {
            port = 27000;// Network.getFreeServerPort();
            // } catch (IOException e) {
            // port = 27000;
            // }
        }
        return port;
    }

    /**
     * internal starter. Not to be invoked by externals
     */
    @BeforeClass
    public static void start()
    {
        _start( true );
    }

    /**
     * start MongoDB
     */
    public static void startMongo()
    {
        _start( false );
    }

    /**
     * actual starter method
     *
     * @param suite started from suite or not, tells if mongo will be started/stopped actually
     */
    private static synchronized void _start( boolean suite )
    {
        if ( !running )
        {
            suiteStarted = suite;
            try
            {
                // with found system MongoDB process skip starting own
                if ( getPort() != 27017 )
                {
                    exe = mongodExecutable();
                    exe.start();
                }
                running = true;
            }
            catch ( Exception e )
            {
                exe = null;
                throw Throwables.propagate( e );
            }
        }
    }

    /**
     * internal stopper. Not to be invoked by externals
     */
    @AfterClass
    public static void stop()
    {
        _stop( true );
    }

    /**
     * stop MongoDB
     */
    public static void stopMongo()
    {
        _stop( false );
    }

    /**
     * actual stoper method
     *
     * @param suite started from suite or not, tells if mongo will be started/stopped actually
     */
    private static synchronized void _stop( boolean suite )
    {
        if ( running && suite == suiteStarted )
        {
            if ( exe != null )
            {
                exe.stop();
                exe = null;
            }
            running = false;
        }
    }
}
