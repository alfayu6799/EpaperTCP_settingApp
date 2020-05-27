/*
 * Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
 *
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 *    Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.yonglin.pdc.epaper_setting.data;

public enum SecurityType {
    OPEN("OPEN"),
    WEP("WEP"),
    PSK("PSK"),
    EAP("EAP");

    private String theState;

    SecurityType(String aState) {
        theState = aState;
    }

    @Override
    public String toString() {
        return theState;
    }

    public static int getIntValue(SecurityType type) {
        switch (type) {
            case OPEN:
                return 0;
            case WEP:
                return 1;
            case PSK:
                return 2;
            case EAP:
                return 3;
            default:
                return 0;
        }
    }

    public static String getStringValue(SecurityType type) {
        switch (type) {
            case WEP:
                return "WEP";
            case PSK:
                return "PSK";
            case EAP:
                return "EAP";
            case OPEN:
            default:
                return "OPEN";
        }
    }

    public static SecurityType parseInt(int value) {
        switch (value) {
            case 1:
                return WEP;
            case 2:
                return PSK;
            case 3:
                return EAP;
            case 0:
            default:
                return OPEN;
        }
    }

    public static SecurityType parseString(String value) {
        SecurityType securityType = OPEN;

        if (value.equals("OPEN"))
            securityType = OPEN;
        else if (value.equals("WEP"))
            securityType = WEP;
        else if (value.equals("PSK"))
            securityType = PSK;
        else if (value.equals("EAP"))
            securityType = EAP;

        return securityType;
    }


}
