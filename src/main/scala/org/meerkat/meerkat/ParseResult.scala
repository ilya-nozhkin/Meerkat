/*
 * Copyright (c) 2014 CWI. All rights reserved.
 * 
 * Authors:
 *     Anastasia Izmaylova  <anastasia.izmaylova@cwi.nl>
 *     Ali Afroozeh         <ali.afroozeh@cwi.nl>
 */
package org.meerkat.meerkat

import org.meerkat.sppf.SPPFNode
import sun.util.locale.ParseStatus

case class ParseSuccess(sppf: SPPFNode, stat: ParseStatistics)
		  				  
case class ParseError(index: Int, slot: String)

case class ParseStatistics(nanoTime: Long, 
                           userTime: Long,
                           systemTime: Long,
                           countNonterminaldNodes: Int,
                           countIntermediateNodes: Int,
                           countTerminalNodes: Int,
                           countPackedNodes: Int,
                           countAmbiguousNodes: Int)