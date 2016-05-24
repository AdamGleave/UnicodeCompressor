package uts6;

/**
 * This sample software accompanies Unicode Technical Report #6 and
 * distributed as is by Unicode, Inc., subject to the following:
 *
 * Copyright � 1996-1997 Unicode, Inc.. All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * without fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * UNICODE, INC. MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.
 * UNICODE, INC., SHALL NOT BE LIABLE FOR ANY ERRORS OR OMISSIONS, AND
 * SHALL NOT BE LIABLE FOR ANY DAMAGES, INCLUDING CONSEQUENTIAL AND
 * INCIDENTAL DAMAGES, SUFFERED BY YOU AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 *  @author Asmus Freytag
 *
 *  @version 001 Dec 25 1996
 *  @version 002 Jun 25 1997
 *  @version 003 Jul 25 1997
 *  @version 004 Aug 25 1997
 *
 * Unicode and the Unicode logo are trademarks of Unicode, Inc.,
 * and are registered in some jurisdictions.
 **/

 /**
    This version of the reference decoder displays the commands tags
    interspersed into the decoded character stream to help analyze the
    performance of an encoder
**/
public class Display extends Expand
{
    static final String uCommand[] = {
    "<UC0>",// Select window 0
    "<UC1>",// Select window 1
    "<UC2>",// Select window 2
    "<UC3>",// Select window 3
    "<UC4>",// Select window 4
    "<UC5>",// Select window 5
    "<UC6>",// Select window 6
    "<UC7>",// Select window 7
    "<UD0>",// Define and select window 0
    "<UD1>",// Define and select window 1
    "<UD2>",// Define and select window 2
    "<UD3>",// Define and select window 3
    "<UD4>",// Define and select window 4
    "<UD5>",// Define and select window 5
    "<UD6>",// Define and select window 6
    "<UD7>",// Define and select window 7
    "<UQU>",// Quote a single Unicode character
    "<UDX>",// Define a window as extended
    "<Urs>"// reserved
    };
    static final String sCommand[] = {
        "<NUL>", // Nul characer
        "<SQ0>",// Quote from window pair 0
        "<SQ1>",// Quote from window pair 1
        "<SQ2>",// Quote from window pair 2
        "<SQ3>",// Quote from window pair 3
        "<SQ4>",// Quote from window pair 4
        "<SQ5>",// Quote from window pair 5
        "<SQ6>",// Quote from window pair 6
        "<SQ7>",// Quote from window pair 7

        "<TAB>",// reserved
        "<NL>", // reserved
        "<SDX>",// Define a window as extended
        "<Srs>",// reserved
        "<CR>", // reserved

        "<SQU>",// Quote a single Unicode character
        "<SCU>",// Change to Unicode mode
        "<SC0>",// Select window
        "<SC1>",// Select window 1
        "<SC2>",// Select window 2
        "<SC3>",// Select window 3
        "<SC4>",// Select window 4
        "<SC5>",// Select window 5
        "<SC6>",// Select window 6
        "<SC7>",// Select window 7
        "<SD0>",// Define and select window 0
        "<SD1>",// Define and select window 1
        "<SD2>",// Define and select window 2
        "<SD3>",// Define and select window 3
        "<SD4>",// Define and select window 4
        "<SD5>",// Define and select window 5
        "<SD6>",// Define and select window 6
        "<SD7>"// Define and select window 7
    };

    private void output(String command, StringBuffer sb)
    {
        sb.append(command);
        iOut += command.length();
    }
    /** expand input that is in Unicode mode
    @param in input byte array to be expanded
    @param iCur starting index
    @param sb string buffer to which to append expanded input
    @return the index for the lastc byte processed
    **/
    protected int expandUnicode(byte []in, int iCur, StringBuffer sb)
        throws IllegalInputException, EndOfInputException
    {
        for( ; iCur < in.length-1; iCur+=2 ) // step by 2:
        {
            byte b = in[iCur];

            if (b >= UC0 && b <= UC7)
            {
                output(uCommand[b - UC0], sb);
                selectWindow(b - UC0);
                return iCur;
            }
            else if (b >= UD0 && b <= UD7)
            {
                output(uCommand[b - UC0], sb);
                defineWindow( b - UD0, in[iCur+1]);
                return iCur + 1;
            }
            else if (b == UDX)
            {
                output(uCommand[b - UC0], sb);
                if( iCur >= in.length - 2)
                {
                    break; // buffer error
                }
                defineExtendedWindow(charFromTwoBytes(in[iCur+1], in[iCur+2]));
                return iCur + 2;
            }
            else if (b == UQU)
            {
                output(uCommand[b - UC0], sb);
                if( iCur >= in.length - 2)
                {
                    break; // error
                }
                // Skip command byte and output Unicode character
                iCur++;
            }

            // output a Unicode character
            char ch = charFromTwoBytes(in[iCur], in[iCur+1]);
            sb.append((char)ch);
            iOut++;
        }

        if( iCur == in.length)
        {
            return iCur;
        }

        // Error condition
        throw new EndOfInputException();
    }


    /** expand portion of the input that is in single byte mode **/
    protected String expandSingleByte(byte []in)
        throws IllegalInputException, EndOfInputException
    {

        /* Allocate the output buffer. Because of control codes, generally
        each byte of input results in fewer than one character of
        output. Using in.length as an intial allocation length should avoid
        the need to reallocate in mid-stream. The exception to this rule are
        surrogates. */
        StringBuffer sb = new StringBuffer(in.length);
        iOut = 0;

        // Loop until all input is exhausted or an error occurred
        int iCur;
        Loop:
        for( iCur = 0; iCur < in.length; iCur++ )
        {
            // DEBUG Debug.out("Expanding: ", iCur);

            // Default behaviour is that ASCII characters are passed through
            // (staticOffset[0] == 0) and characters with the high bit on are
            // offset by the current dynamic (or sliding) window (this.iWindow)
            int iStaticWindow = 0;
            int iDynamicWindow = getCurrentWindow();

            switch(in[iCur])
            {
                // Quote from a static Window
            case SQ0:
            case SQ1:
            case SQ2:
            case SQ3:
            case SQ4:
            case SQ5:
            case SQ6:
            case SQ7:
                output(sCommand[in[iCur]], sb);
                // skip the command byte and check for length
                if( iCur >= in.length - 1)
                {
                    Debug.out("SQn missing argument: ", in, iCur);
                    break Loop;  // buffer length error
                }
                // Select window pair to quote from
                iDynamicWindow = iStaticWindow = in[iCur] - SQ0;
                iCur ++;

                // FALL THROUGH

            default:
                // output as character
                if(in[iCur] >= 0)
                {
                    // use static window
                    int ch = in[iCur] + staticOffset[iStaticWindow];
                    sb.append((char)ch);
                    iOut++;
                }
                else
                {
                    // use dynamic window
                    int ch = (in[iCur] + 256); // adjust for signed bytes
                    ch -= 0x80;                // reduce to range 00..7F
                    ch += dynamicOffset[iDynamicWindow];

                    //DEBUG
                    Debug.out("Dynamic: ", (char) ch);

                    if (ch < 1<<16)
                    {
                        // in Unicode range, output directly
                        sb.append((char)ch);
                        iOut++;
                    }
                    else
                    {
                        // this is an extension character
                        Debug.out("Extension character: ", ch);

                        // compute and append the two surrogates:
                        // translate from 10000..10FFFF to 0..FFFFF
                        ch -= 0x10000;

                        // high surrogate = top 10 bits added to D800
                        sb.append((char)(0xD800 + (ch>>10)));
                        iOut++;

                        // low surrogate = bottom 10 bits added to DC00
                        sb.append((char)(0xDC00 + (ch & ~0xFC00)));
                        iOut++;
                    }
                }
                break;

                // define a dynamic window as extended
            case SDX:
                output(sCommand[in[iCur]], sb);
                iCur += 2;
                if( iCur >= in.length)
                {
                    Debug.out("SDn missing argument: ", in, iCur -1);
                    break Loop;  // buffer length error
                }
                defineExtendedWindow(charFromTwoBytes(in[iCur-1], in[iCur]));
                break;

                // Position a dynamic Window
            case SD0:
            case SD1:
            case SD2:
            case SD3:
            case SD4:
            case SD5:
            case SD6:
            case SD7:
                output(sCommand[in[iCur]], sb);
                iCur ++;
                if( iCur >= in.length)
                {
                    Debug.out("SDn missing argument: ", in, iCur -1);
                    break Loop;  // buffer length error
                }
                defineWindow(in[iCur-1] - SD0, in[iCur]);
                break;

                // Select a new dynamic Window
            case SC0:
            case SC1:
            case SC2:
            case SC3:
            case SC4:
            case SC5:
            case SC6:
            case SC7:
                output(sCommand[in[iCur]], sb);
                selectWindow(in[iCur] - SC0);
                break;
            case SCU:
                output(sCommand[in[iCur]], sb);
                // switch to Unicode mode and continue parsing
                iCur = expandUnicode(in, iCur+1, sb);
                // DEBUG Debug.out("Expanded Unicode range until: ", iCur);
                break;

            case SQU:
                output(sCommand[in[iCur]], sb);
                // directly extract one Unicode character
                iCur += 2;
                if( iCur >= in.length)
                {
                     Debug.out("SQU missing argument: ", in, iCur - 2);
                     break Loop;  // buffer length error
                }
                else
                {
                    char ch = charFromTwoBytes(in[iCur-1], in[iCur]);

                    Debug.out("Quoted: ", ch);
                    sb.append((char)ch);
                    iOut++;
                }
                break;

             case Srs:
                output(sCommand[in[iCur]], sb);
                throw new IllegalInputException();
                // break;
            }
        }

        if( iCur >= in.length)
        {
            //SUCCESS: all input used up
            sb.setLength(iOut);
            iIn = iCur;
            return sb.toString();
        }

        Debug.out("Length ==" + in.length+" iCur =", iCur);
        //ERROR: premature end of input
        throw new EndOfInputException();
    }
}
