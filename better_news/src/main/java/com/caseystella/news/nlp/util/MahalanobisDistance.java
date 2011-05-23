package com.caseystella.news.nlp.util;

import java.io.Serializable;

import org.apache.commons.math.DimensionMismatchException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealMatrixImpl;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;

@SuppressWarnings("deprecation")
public class MahalanobisDistance implements Serializable
{

    /**
	 * 
	 */
	private static final long serialVersionUID = 7431191803229470786L;
	//--------------------------------------------------------------------
    private final double sums[];
    private       double count;
    private       double means[];

    private final VectorialCovariance covariance;
    private       RealMatrix          covarianceMatrix;


    //--------------------------------------------------------------------
    public MahalanobisDistance(int dimensions)
    {
        sums             = new double[dimensions];
        means            = null;

        covariance       = new VectorialCovariance(dimensions, false);
        covarianceMatrix = null;
    }


    //--------------------------------------------------------------------
    public void add(double values[], double valueCount)
    {
        for (int i = 0; i < valueCount; i++)
        {
            add(values);
        }
    }
    public void add(double values[])
    {
        covarianceMatrix = null;

        try {
            covariance.increment(values);
        } catch (DimensionMismatchException e) {
            throw new Error( e );
        }

        for (int i = 0; i < values.length; i++)
        {
            sums[ i ] += values[ i ];
        }
        count++;
    }
    RealMatrix inverseCovarianceMatrix;
    RealMatrix meanMatrix;
    public void finalizeDistance()
    {
    	if (covarianceMatrix == null)
        {
            covarianceMatrix = covariance.getResult();
        }

        if (means == null)
        {
            means = new double[ sums.length ];
            for (int i = 0; i < means.length; i++)
            {
                means[ i ] = sums[ i ] / count;
            }
        }
        meanMatrix = new RealMatrixImpl(means);
        inverseCovarianceMatrix = covarianceMatrix.inverse();
    }

    //--------------------------------------------------------------------
    @SuppressWarnings("deprecation")
	public double distance(double to[])
    {
        assert count > 0;

        
        RealMatrix xT = new RealMatrixImpl(to   );

        RealMatrix xMinusU  = xT.subtract( meanMatrix );
        
        RealMatrix distSquared =
                xMinusU.transpose()
                    .multiply( inverseCovarianceMatrix )
                .multiply( xMinusU );

        double ret =  Math.sqrt(distSquared.getEntry(0, 0));
        return ret;
    }
    
}
