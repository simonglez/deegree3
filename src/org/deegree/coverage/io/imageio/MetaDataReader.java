//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

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
package org.deegree.coverage.io.imageio;

import javax.imageio.metadata.IIOMetadata;

import org.deegree.coverage.raster.geom.RasterEnvelope;
import org.deegree.crs.CRSRegistry;
import org.deegree.crs.coordinatesystems.CoordinateSystem;
import org.deegree.crs.exceptions.UnknownCRSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author <a href="mailto:tonnhofer@lat-lon.de">Oliver Tonnhofer</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 * 
 */
public class MetaDataReader {

    private static final String TIFF_MD_FORMAT = "com_sun_media_imageio_plugins_tiff_image_1.0";

    private IIOMetadata metaData;

    private RasterEnvelope rasterEnvelope = null;

    private CoordinateSystem crs = null;

    private static Logger LOG = LoggerFactory.getLogger( MetaDataReader.class );

    /**
     * @param metaData
     *            a ImageIO meta data object
     */
    public MetaDataReader( IIOMetadata metaData ) {
        this.metaData = metaData;
        if ( metaData != null ) {
            init();
        }
    }

    /**
     * @return the raster envelope or <code>null</code>, if the metadata contains no georeference
     */
    public RasterEnvelope getRasterEnvelope() {
        return rasterEnvelope;
    }

    /**
     * @return the coordinate system or <code>null</code>, if the metadata contains no crs
     */
    public CoordinateSystem getCRS() {
        return crs;
    }

    private void init() {
        if ( metaData.getNativeMetadataFormatName().equals( TIFF_MD_FORMAT ) ) {
            initGeoTIFF();
        }

    }

    // read GeoTIFF metadata
    private void initGeoTIFF() {
        GeoTiffIIOMetadataAdapter geoTIFFMetaData = new GeoTiffIIOMetadataAdapter( metaData );

        try {
            int modelType = Integer.valueOf( geoTIFFMetaData.getGeoKey( GeoTiffIIOMetadataAdapter.GTModelTypeGeoKey ) );
            String epsgCode = null;
            if ( modelType == GeoTiffIIOMetadataAdapter.ModelTypeProjected ) {
                epsgCode = geoTIFFMetaData.getGeoKey( GeoTiffIIOMetadataAdapter.ProjectedCSTypeGeoKey );
            } else if ( modelType == GeoTiffIIOMetadataAdapter.ModelTypeGeographic ) {
                epsgCode = geoTIFFMetaData.getGeoKey( GeoTiffIIOMetadataAdapter.GeographicTypeGeoKey );
            }
            if ( epsgCode != null && epsgCode.length() != 0 ) {
                try {
                    crs = CRSRegistry.lookup( "EPSG:" + epsgCode );
                } catch ( UnknownCRSException e ) {
                    LOG.error( "No coordinate system found for EPSG:" + epsgCode );
                }
            }
        } catch ( UnsupportedOperationException ex ) {
            LOG.debug( "couldn't read crs information in GeoTIFF" );
        }

        try {
            double[] tiePoints = geoTIFFMetaData.getModelTiePoints();
            double[] scale = geoTIFFMetaData.getModelPixelScales();
            if ( tiePoints != null && scale != null ) {
                if ( Math.abs( scale[0] - 0.5 ) < 0.001 ) { // when first pixel tie point is 0.5 -> center type
                    rasterEnvelope = new RasterEnvelope( RasterEnvelope.Type.CENTER, tiePoints[3], tiePoints[4],
                                                         scale[0], -scale[1] );
                } else {
                    rasterEnvelope = new RasterEnvelope( RasterEnvelope.Type.OUTER, tiePoints[3], tiePoints[4],
                                                         scale[0], -scale[1] );
                }

            }
        } catch ( UnsupportedOperationException ex ) {
            LOG.debug( "couldn't read georeference information in GeoTIFF" );
        }

        if ( LOG.isDebugEnabled() ) {
            for ( String format : metaData.getMetadataFormatNames() ) {
                // IIOMetadataNode elem = (IIOMetadataNode) metaData.getAsTree( format );
                LOG.debug( "metadata format: " + format );
                LOG.debug( "TBD output the xml file here." );
                // LogUtils.writeTempFile( LOG, "geotiff", ".xml", new XMLFragment( elem ).toString() );
            }
        }
    }
}
