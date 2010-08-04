package org.sonatype.tycho.plugins.p2.publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.p2.MetadataSerializable;
import org.codehaus.tycho.testing.TestUtil;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.plugins.p2.publisher.PublishProductMojo.Product;

public class PublishProductMojoUnitTest
{
    private PublishProductMojo subject;

    private File tempDir;

    private File sourceDirectory;

    private File targetDirectory;

    @Before
    public void setUp()
        throws IOException
    {
        tempDir = createTempDir( getClass().getSimpleName() );
        sourceDirectory = new File( tempDir, "source" );
        sourceDirectory.mkdirs();
        targetDirectory = new File( tempDir, "target" );
        targetDirectory.mkdirs();
        subject = new PublishProductMojo();
    }

    @After
    public void tearDown()
        throws IOException
    {
        FileUtils.deleteDirectory( tempDir );
    }

    @Test
    public void testQualifyVersions()
        throws IOException
    {
        File basedir = TestUtil.getBasedir( "unitTestResources" );
        File productFile = new File( basedir, "test.product" );
        ProductConfiguration product = ProductConfiguration.read( productFile );
        PublishProductMojo.qualifyVersions( product, "20100623" );

        Assert.assertEquals( "0.1.0.20100623", product.getVersion() );
        Assert.assertEquals( "0.1.0.20100623", product.getFeatures().get( 0 ).getVersion() );
        Assert.assertEquals( "0.1.0.qual", product.getFeatures().get( 1 ).getVersion() );
        Assert.assertEquals( "0.1.0.20100623", product.getPlugins().get( 0 ).getVersion() );
        Assert.assertEquals( "0.1.0.qual", product.getPlugins().get( 1 ).getVersion() );
    }

    @Test
    public void testQualifyVersionsNoVersions()
        throws IOException
    {
        File basedir = TestUtil.getBasedir( "unitTestResources" );
        File productFile = new File( basedir, "noVersion.product" );
        ProductConfiguration product = ProductConfiguration.read( productFile );
        PublishProductMojo.qualifyVersions( product, "20100623" );

        Assert.assertNull( product.getVersion() );
    }

    @Test
    public void testQualifyVersionsEmptyVersions()
        throws IOException
    {
        File basedir = TestUtil.getBasedir( "unitTestResources" );
        File productFile = new File( basedir, "emptyVersion.product" );
        ProductConfiguration product = ProductConfiguration.read( productFile );
        PublishProductMojo.qualifyVersions( product, "20100623" );

        Assert.assertEquals( "", product.getVersion() );
    }

    @Test
    public void testPrepareBuildProduct()
        throws IOException
    {
        File basedir = TestUtil.getBasedir( "unitTestResources" );
        File productFile = new File( basedir, "test.product" );
        Product product = new Product( productFile );
        File buildBasedir = new File( tempDir, "buildBasedir" );
        Product buildProduct = PublishProductMojo.prepareBuildProduct( product, buildBasedir, "buildQualifier" );

        Assert.assertEquals( new File( buildBasedir, "products/testproduct/p2.inf" ), buildProduct.getP2infFile() );
        Assert.assertTrue( new File( buildBasedir, "products/testproduct/p2.inf" ).exists() );

        ProductConfiguration productConfiguration = ProductConfiguration.read( buildProduct.getProductFile() );
        Assert.assertEquals( "0.1.0.buildQualifier", productConfiguration.getVersion() );
    }

    @Test
    public void testCopyMissingP2Inf()
        throws IOException
    {
        File productFile = new File( sourceDirectory, "test.product" );
        productFile.createNewFile();

        File p2InfTarget = new File(targetDirectory,"p2.inf");
        PublishProductMojo.copyP2Inf( Product.getSourceP2InfFile( productFile), p2InfTarget );
        Assert.assertFalse( p2InfTarget.exists() );
    }

    @Test
    public void testGetSourceP2InfFile()
    {
        File p2InfFile = Product.getSourceP2InfFile( new File( "./test/test.product" ) );
        Assert.assertEquals( new File( "./test/test.p2.inf" ), p2InfFile );
    }

    @Test
    public void testGetConfigsParameter()
    {
        String[] configsParameter =
            subject.getConfigsParameter( Arrays.asList( new TargetEnvironment( "os", "ws", "arch", "nl" ) ) );
        Assert.assertEquals( "-configs", configsParameter[0] );
        Assert.assertEquals( "ws.os.arch", configsParameter[1] );
    }

    @Test
    public void testGetConfigsParameterTwoOSes()
    {
        String[] configsParameter =
            subject.getConfigsParameter( Arrays.asList( new TargetEnvironment( "os", "ws", "arch", "nl" ),
                                                        new TargetEnvironment( "os2", "ws", "arch", "nl2" ) ) );
        Assert.assertEquals( "-configs", configsParameter[0] );
        Assert.assertEquals( "ws.os.arch,ws.os2.arch", configsParameter[1] );
    }

    @Test
    public void testCreateContextRepositoryUrl()
        throws IOException
    {
        MetadataSerializable metadataRepositorySerializable = EasyMock.createMock( MetadataSerializable.class );
        metadataRepositorySerializable.serialize( EasyMock.isA( OutputStream.class ) );
        EasyMock.expectLastCall().andAnswer( new IAnswer<Void>()
        {

            public Void answer()
                throws Throwable
            {
                OutputStream os = (OutputStream) EasyMock.getCurrentArguments()[0];
                os.write( "content.xml".getBytes() );
                return null;
            }

        } );

        EasyMock.replay( metadataRepositorySerializable );

        subject.materializeRepository( metadataRepositorySerializable, tempDir );

        File contentXml = new File( tempDir, "targetMetadataRepository/content.xml" );
        Assert.assertTrue( contentXml.exists() );
        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( contentXml ) ) );
        try
        {
            Assert.assertEquals( "content.xml", reader.readLine() );
        }
        finally
        {
            reader.close();
        }

        EasyMock.verify( metadataRepositorySerializable );
    }

    @Test
    public void testCreateOSGiCommandline()
    {
        File equinoxLauncher = new File( "equinoxLauncher" );
        String expectedLauncherLocation = equinoxLauncher.getAbsolutePath();
        Commandline commandline =
            PublishProductMojo.createOSGiCommandline( "application.id", equinoxLauncher );
        Assert.assertEquals( new String[] { "-jar", expectedLauncherLocation, "-application", "application.id",
            "-nosplash", "-consoleLog" }, commandline.getArguments() );
    }

    private File createTempDir( String prefix )
        throws IOException
    {
        File directory = File.createTempFile( prefix, "" );
        if ( directory.delete() )
        {
            directory.mkdirs();
            return directory;
        }
        else
        {
            throw new IOException( "Could not create temp directory at: " + directory.getAbsolutePath() );
        }
    }

}
