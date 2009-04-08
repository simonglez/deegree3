//$HeadURL:svn+ssh://otonnhofer@svn.wald.intevation.org/deegree/deegree3/model/trunk/src/org/deegree/model/coverage/raster/implementation/io/WorldFileReader.java $
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/
package org.deegree.coverage.raster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.deegree.commons.utils.FileUtils;
import org.deegree.coverage.raster.geom.RasterEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representation of a ESRI world file. A world file may defines bounding coordinates centered on the outer pixel
 * (e.g. ESRI software) or outside the bounding pixels (e.g.Oracle spatial). Reading a worldfile this must be considered
 * so the type of a worldfile must be passed. For this a <code>enum</code> named <code>TYPE</code> ist defined.
 * 
 * 
 * @author <a href="mailto:poth@lat-lon.de">Andreas Poth</a>
 * @author last edited by: $Author:otonnhofer $
 * 
 * @version Modified from Revision: 7587 $ Date: 2007-06-19 11:29:12 +0200 (Tue, 19 Jun 2007) $
 * @version $Revision:10872 $, $Date:2008-04-01 15:41:48 +0200 (Tue, 01 Apr 2008) $
 */
public class WorldFileAccess {

    private static Logger log = LoggerFactory.getLogger( WorldFileAccess.class );

    /**
     * <code>TYPE</code> enumerates the world file types.
     * 
     * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
     * @author last edited by: $Author:otonnhofer $
     * 
     * @version $Revision:10872 $, $Date:2008-04-01 15:41:48 +0200 (Tue, 01 Apr 2008) $
     */
    public enum TYPE {

        /**
         * Coordinates denote pixel centers.
         */
        CENTER,

        /**
         * Coordinates denote outer edges.
         */
        OUTER

    }

    private static final String[] WORLD_FILE_EXT = new String[] { "wld", "tfw", "tifw", "jgw", "jpgw", "gfw", "gifw",
                                                                 "pgw", "pngw" };

    /**
     * Returns the world file for given file.
     * 
     * @param rasterFile
     *            the raster file
     * @return the world file or null if not found.
     */
    private static File getWorldFile( File rasterFile ) {
        String basename = FileUtils.getBasename( rasterFile );
        // Look for corresponding worldfiles.
        String wldName = "";
        for ( String ext : WORLD_FILE_EXT ) {
            String tmp = basename + "." + ext;
            if ( new File( tmp ).exists() ) {
                wldName = tmp;
                break;
            }
        }
        return new File( wldName );
    }

    /**
     * @param filename
     *            the image/raster file (including path and file extension)
     * @param type
     * @return a RasterEnvelope
     * @throws IOException
     */
    public static RasterEnvelope readWorldFile( File filename, TYPE type )
                            throws IOException {

        File worldFile = getWorldFile( filename );
        if ( !worldFile.exists() ) {
            throw new IOException( "No world file for: " + filename );
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "read worldfile for " + filename );
        }

        BufferedReader br = new BufferedReader( new FileReader( worldFile ) );
        double[] values = new double[6];

        try {
            for ( int i = 0; i < 6; i++ ) {
                String line = br.readLine();
                if ( line == null ) {
                    throw new IOException( "invalid world file (" + worldFile.getAbsolutePath() + ")" );
                }
                line = line.trim();
                double val = Double.parseDouble( line.replace( ',', '.' ) );
                values[i] = val;
            }
        } catch ( NumberFormatException e ) {
            throw new IOException( "invalid world file (" + worldFile.getAbsolutePath() + ")" );
        }

        br.close();

        double resx = values[0];
        double resy = values[3];
        double xmin = values[4];
        double ymax = values[5];
        // double xmax = xmin + ( ( width - 1 ) * resx );
        // double ymin = ymax + ( ( height - 1 ) * resy );

        if ( type == TYPE.OUTER ) {
            xmin = xmin + resx / 2.0;
            // ymin = ymin - resy / 2.0;
            // xmax = xmax - resx / 2.0;
            ymax = ymax + resy / 2.0;
        }

        return new RasterEnvelope( xmin, ymax, resx, resy );
    }

    /**
     * writes a RasterEnvelope into a world file (with .wld extension).
     * 
     * @param renv
     *            the envelope
     * @param file
     *            the raster file
     * @throws IOException
     */
    public static void writeWorldFile( RasterEnvelope renv, File file )
                            throws IOException {
        writeWorldFile( renv, file, "wld" );
    }

    /**
     * writes a RasterEnvelope into a world file.
     * 
     * @param renv
     *            the envelope
     * @param file
     *            the raster file
     * @param extension
     *            the file extension for the world file (eg. 'wld', 'tfw', etc)
     * @throws IOException
     */
    public static void writeWorldFile( RasterEnvelope renv, File file, String extension )
                            throws IOException {

        StringBuffer sb = new StringBuffer();

        sb.append( renv.getXRes() ).append( "\n" ).append( 0.0 ).append( "\n" );
        sb.append( 0.0 ).append( "\n" ).append( renv.getYRes() ).append( "\n" );
        sb.append( renv.getX0( RasterEnvelope.Type.CENTER ) ).append( "\n" );
        sb.append( renv.getY0( RasterEnvelope.Type.CENTER ) ).append( "\n" );

        File f = new File( FileUtils.getBasename( file ) + "." + extension );

        FileWriter fw = new FileWriter( f );
        PrintWriter pw = new PrintWriter( fw );

        pw.print( sb.toString() );

        pw.close();
        fw.close();
    }

}
