package com.jprcoder.valnarratorbackend;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

enum HtmlEscapeType {

    /**
     * Use HTML 4 NCRs if possible, default to Decimal Character References.
     */
    HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false, false),

    /**
     * Use HTML 4 NCRs if possible, default to Hexadecimal Character References.
     */
    HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true, false),

    /**
     * Use HTML5 NCRs if possible, default to Decimal Character References.
     */
    HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false, true),

    /**
     * Use HTML5 NCRs if possible, default to Hexadecimal Character References.
     */
    HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true, true),

    /**
     * Always use Decimal Character References (no NCRs will be used).
     */
    DECIMAL_REFERENCES(false, false, false),

    /**
     * Always use Hexadecimal Character References (no NCRs will be used).
     */
    HEXADECIMAL_REFERENCES(false, true, false);


    private final boolean useNCRs;
    private final boolean useHexa;
    private final boolean useHtml5;

    HtmlEscapeType(final boolean useNCRs, final boolean useHexa, final boolean useHtml5) {
        this.useNCRs = useNCRs;
        this.useHexa = useHexa;
        this.useHtml5 = useHtml5;
    }

    boolean getUseNCRs() {
        return this.useNCRs;
    }

    boolean getUseHexa() {
        return this.useHexa;
    }

    boolean getUseHtml5() {
        return this.useHtml5;
    }
}


enum HtmlEscapeLevel {

    /**
     * Level 0 escape: escape only markup-significant characters, excluding the apostrophe:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt> and <tt>&quot;</tt>
     */
    LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS(0),

    /**
     * Level 1 escape (<em>XML-style</em>): escape only markup-significant characters (including the apostrophe):
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>
     */
    LEVEL_1_ONLY_MARKUP_SIGNIFICANT(1),

    /**
     * Level 2 escape: escape markup-significant characters plus all non-ASCII characters (result will always be ASCII).
     */
    LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT(2),

    /**
     * Level 3 escape: escape all non-alphanumeric characteres (escape all but those in the
     * <tt>A</tt>-<tt>Z</tt>, <tt>a</tt>-<tt>z</tt> and <tt>0</tt>-<tt>9</tt> ranges).
     */
    LEVEL_3_ALL_NON_ALPHANUMERIC(3),

    /**
     * Level 4 escape: escape all characters, including alphanumeric.
     */
    LEVEL_4_ALL_CHARACTERS(4);


    private final int escapeLevel;


    HtmlEscapeLevel(final int escapeLevel) {
        this.escapeLevel = escapeLevel;
    }

    /**
     * <p>
     * Utility method for obtaining an enum value from its corresponding <tt>int</tt> level value.
     * </p>
     *
     * @param level the level
     * @return the escape level enum constant, or <tt>IllegalArgumentException</tt> if level does not exist.
     */
    public static HtmlEscapeLevel forLevel(final int level) {
        switch (level) {
            case 0:
                return LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS;
            case 1:
                return LEVEL_1_ONLY_MARKUP_SIGNIFICANT;
            case 2:
                return LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT;
            case 3:
                return LEVEL_3_ALL_NON_ALPHANUMERIC;
            case 4:
                return LEVEL_4_ALL_CHARACTERS;
            default:
                throw new IllegalArgumentException("No escape level enum constant defined for level: " + level);
        }
    }

    /**
     * Return the <tt>int</tt> escape level.
     *
     * @return the escape level.
     */
    public int getEscapeLevel() {
        return this.escapeLevel;
    }

}

final class Html5EscapeSymbolsInitializer {


    private Html5EscapeSymbolsInitializer() {
        super();
    }

    static HtmlEscapeSymbols initializeHtml5() {

        final HtmlEscapeSymbols.References html5References = new HtmlEscapeSymbols.References();

        /*
         * --------------------------------------------------------------------------------------------------
         *   HTML5 NAMED CHARACTER REFERENCES
         *   See: http://www.w3.org/TR/html5/syntax.html#named-character-references  [HTML5]
         *        http://www.w3.org/TR/html51/syntax.html#named-character-references [HTML 5.1]
         * --------------------------------------------------------------------------------------------------
         */
        html5References.addReference(9, "&Tab;");
        html5References.addReference(10, "&NewLine;");
        html5References.addReference(33, "&excl;");
        html5References.addReference(34, "&quot;");
        html5References.addReference(34, "&quot");
        html5References.addReference(34, "&QUOT");
        html5References.addReference(34, "&QUOT;");
        html5References.addReference(35, "&num;");
        html5References.addReference(36, "&dollar;");
        html5References.addReference(37, "&percnt;");
        html5References.addReference(38, "&amp;");
        html5References.addReference(38, "&amp");
        html5References.addReference(38, "&AMP");
        html5References.addReference(38, "&AMP;");
        html5References.addReference(39, "&apos;");
        html5References.addReference(40, "&lpar;");
        html5References.addReference(41, "&rpar;");
        html5References.addReference(42, "&ast;");
        html5References.addReference(42, "&midast;");
        html5References.addReference(43, "&plus;");
        html5References.addReference(44, "&comma;");
        html5References.addReference(46, "&period;");
        html5References.addReference(47, "&sol;");
        html5References.addReference(58, "&colon;");
        html5References.addReference(59, "&semi;");
        html5References.addReference(60, "&lt;");
        html5References.addReference(60, "&lt");
        html5References.addReference(60, "&LT");
        html5References.addReference(60, "&LT;");
        html5References.addReference(60, 8402, "&nvlt;");
        html5References.addReference(61, "&equals;");
        html5References.addReference(61, 8421, "&bne;");
        html5References.addReference(62, "&gt;");
        html5References.addReference(62, "&gt");
        html5References.addReference(62, "&GT");
        html5References.addReference(62, "&GT;");
        html5References.addReference(62, 8402, "&nvgt;");
        html5References.addReference(63, "&quest;");
        html5References.addReference(64, "&commat;");
        html5References.addReference(91, "&lbrack;");
        html5References.addReference(91, "&lsqb;");
        html5References.addReference(92, "&bsol;");
        html5References.addReference(93, "&rbrack;");
        html5References.addReference(93, "&rsqb;");
        html5References.addReference(94, "&Hat;");
        html5References.addReference(95, "&lowbar;");
        html5References.addReference(95, "&UnderBar;");
        html5References.addReference(96, "&grave;");
        html5References.addReference(96, "&DiacriticalGrave;");
        html5References.addReference(102, 106, "&fjlig;");
        html5References.addReference(123, "&lbrace;");
        html5References.addReference(123, "&lcub;");
        html5References.addReference(124, "&verbar;");
        html5References.addReference(124, "&vert;");
        html5References.addReference(124, "&VerticalLine;");
        html5References.addReference(125, "&rbrace;");
        html5References.addReference(125, "&rcub;");
        html5References.addReference(160, "&nbsp;");
        html5References.addReference(160, "&nbsp");
        html5References.addReference(160, "&NonBreakingSpace;");
        html5References.addReference(161, "&iexcl;");
        html5References.addReference(161, "&iexcl");
        html5References.addReference(162, "&cent;");
        html5References.addReference(162, "&cent");
        html5References.addReference(163, "&pound;");
        html5References.addReference(163, "&pound");
        html5References.addReference(164, "&curren;");
        html5References.addReference(164, "&curren");
        html5References.addReference(165, "&yen;");
        html5References.addReference(165, "&yen");
        html5References.addReference(166, "&brvbar;");
        html5References.addReference(166, "&brvbar");
        html5References.addReference(167, "&sect;");
        html5References.addReference(167, "&sect");
        html5References.addReference(168, "&uml;");
        html5References.addReference(168, "&die;");
        html5References.addReference(168, "&uml");
        html5References.addReference(168, "&Dot;");
        html5References.addReference(168, "&DoubleDot;");
        html5References.addReference(169, "&copy;");
        html5References.addReference(169, "&copy");
        html5References.addReference(169, "&COPY");
        html5References.addReference(169, "&COPY;");
        html5References.addReference(170, "&ordf;");
        html5References.addReference(170, "&ordf");
        html5References.addReference(171, "&laquo;");
        html5References.addReference(171, "&laquo");
        html5References.addReference(172, "&not;");
        html5References.addReference(172, "&not");
        html5References.addReference(173, "&shy;");
        html5References.addReference(173, "&shy");
        html5References.addReference(174, "&reg;");
        html5References.addReference(174, "&circledR;");
        html5References.addReference(174, "&reg");
        html5References.addReference(174, "&REG");
        html5References.addReference(174, "&REG;");
        html5References.addReference(175, "&macr;");
        html5References.addReference(175, "&macr");
        html5References.addReference(175, "&strns;");
        html5References.addReference(176, "&deg;");
        html5References.addReference(176, "&deg");
        html5References.addReference(177, "&plusmn;");
        html5References.addReference(177, "&plusmn");
        html5References.addReference(177, "&pm;");
        html5References.addReference(177, "&PlusMinus;");
        html5References.addReference(178, "&sup2;");
        html5References.addReference(178, "&sup2");
        html5References.addReference(179, "&sup3;");
        html5References.addReference(179, "&sup3");
        html5References.addReference(180, "&acute;");
        html5References.addReference(180, "&acute");
        html5References.addReference(180, "&DiacriticalAcute;");
        html5References.addReference(181, "&micro;");
        html5References.addReference(181, "&micro");
        html5References.addReference(182, "&para;");
        html5References.addReference(182, "&para");
        html5References.addReference(183, "&middot;");
        html5References.addReference(183, "&centerdot;");
        html5References.addReference(183, "&middot");
        html5References.addReference(183, "&CenterDot;");
        html5References.addReference(184, "&cedil;");
        html5References.addReference(184, "&cedil");
        html5References.addReference(184, "&Cedilla;");
        html5References.addReference(185, "&sup1;");
        html5References.addReference(185, "&sup1");
        html5References.addReference(186, "&ordm;");
        html5References.addReference(186, "&ordm");
        html5References.addReference(187, "&raquo;");
        html5References.addReference(187, "&raquo");
        html5References.addReference(188, "&frac14;");
        html5References.addReference(188, "&frac14");
        html5References.addReference(189, "&frac12;");
        html5References.addReference(189, "&frac12");
        html5References.addReference(189, "&half;");
        html5References.addReference(190, "&frac34;");
        html5References.addReference(190, "&frac34");
        html5References.addReference(191, "&iquest;");
        html5References.addReference(191, "&iquest");
        html5References.addReference(192, "&Agrave;");
        html5References.addReference(192, "&Agrave");
        html5References.addReference(193, "&Aacute;");
        html5References.addReference(193, "&Aacute");
        html5References.addReference(194, "&Acirc;");
        html5References.addReference(194, "&Acirc");
        html5References.addReference(195, "&Atilde;");
        html5References.addReference(195, "&Atilde");
        html5References.addReference(196, "&Auml;");
        html5References.addReference(196, "&Auml");
        html5References.addReference(197, "&Aring;");
        html5References.addReference(197, "&angst;");
        html5References.addReference(197, "&Aring");
        html5References.addReference(198, "&AElig;");
        html5References.addReference(198, "&AElig");
        html5References.addReference(199, "&Ccedil;");
        html5References.addReference(199, "&Ccedil");
        html5References.addReference(200, "&Egrave;");
        html5References.addReference(200, "&Egrave");
        html5References.addReference(201, "&Eacute;");
        html5References.addReference(201, "&Eacute");
        html5References.addReference(202, "&Ecirc;");
        html5References.addReference(202, "&Ecirc");
        html5References.addReference(203, "&Euml;");
        html5References.addReference(203, "&Euml");
        html5References.addReference(204, "&Igrave;");
        html5References.addReference(204, "&Igrave");
        html5References.addReference(205, "&Iacute;");
        html5References.addReference(205, "&Iacute");
        html5References.addReference(206, "&Icirc;");
        html5References.addReference(206, "&Icirc");
        html5References.addReference(207, "&Iuml;");
        html5References.addReference(207, "&Iuml");
        html5References.addReference(208, "&ETH;");
        html5References.addReference(208, "&ETH");
        html5References.addReference(209, "&Ntilde;");
        html5References.addReference(209, "&Ntilde");
        html5References.addReference(210, "&Ograve;");
        html5References.addReference(210, "&Ograve");
        html5References.addReference(211, "&Oacute;");
        html5References.addReference(211, "&Oacute");
        html5References.addReference(212, "&Ocirc;");
        html5References.addReference(212, "&Ocirc");
        html5References.addReference(213, "&Otilde;");
        html5References.addReference(213, "&Otilde");
        html5References.addReference(214, "&Ouml;");
        html5References.addReference(214, "&Ouml");
        html5References.addReference(215, "&times;");
        html5References.addReference(215, "&times");
        html5References.addReference(216, "&Oslash;");
        html5References.addReference(216, "&Oslash");
        html5References.addReference(217, "&Ugrave;");
        html5References.addReference(217, "&Ugrave");
        html5References.addReference(218, "&Uacute;");
        html5References.addReference(218, "&Uacute");
        html5References.addReference(219, "&Ucirc;");
        html5References.addReference(219, "&Ucirc");
        html5References.addReference(220, "&Uuml;");
        html5References.addReference(220, "&Uuml");
        html5References.addReference(221, "&Yacute;");
        html5References.addReference(221, "&Yacute");
        html5References.addReference(222, "&THORN;");
        html5References.addReference(222, "&THORN");
        html5References.addReference(223, "&szlig;");
        html5References.addReference(223, "&szlig");
        html5References.addReference(224, "&agrave;");
        html5References.addReference(224, "&agrave");
        html5References.addReference(225, "&aacute;");
        html5References.addReference(225, "&aacute");
        html5References.addReference(226, "&acirc;");
        html5References.addReference(226, "&acirc");
        html5References.addReference(227, "&atilde;");
        html5References.addReference(227, "&atilde");
        html5References.addReference(228, "&auml;");
        html5References.addReference(228, "&auml");
        html5References.addReference(229, "&aring;");
        html5References.addReference(229, "&aring");
        html5References.addReference(230, "&aelig;");
        html5References.addReference(230, "&aelig");
        html5References.addReference(231, "&ccedil;");
        html5References.addReference(231, "&ccedil");
        html5References.addReference(232, "&egrave;");
        html5References.addReference(232, "&egrave");
        html5References.addReference(233, "&eacute;");
        html5References.addReference(233, "&eacute");
        html5References.addReference(234, "&ecirc;");
        html5References.addReference(234, "&ecirc");
        html5References.addReference(235, "&euml;");
        html5References.addReference(235, "&euml");
        html5References.addReference(236, "&igrave;");
        html5References.addReference(236, "&igrave");
        html5References.addReference(237, "&iacute;");
        html5References.addReference(237, "&iacute");
        html5References.addReference(238, "&icirc;");
        html5References.addReference(238, "&icirc");
        html5References.addReference(239, "&iuml;");
        html5References.addReference(239, "&iuml");
        html5References.addReference(240, "&eth;");
        html5References.addReference(240, "&eth");
        html5References.addReference(241, "&ntilde;");
        html5References.addReference(241, "&ntilde");
        html5References.addReference(242, "&ograve;");
        html5References.addReference(242, "&ograve");
        html5References.addReference(243, "&oacute;");
        html5References.addReference(243, "&oacute");
        html5References.addReference(244, "&ocirc;");
        html5References.addReference(244, "&ocirc");
        html5References.addReference(245, "&otilde;");
        html5References.addReference(245, "&otilde");
        html5References.addReference(246, "&ouml;");
        html5References.addReference(246, "&ouml");
        html5References.addReference(247, "&divide;");
        html5References.addReference(247, "&div;");
        html5References.addReference(247, "&divide");
        html5References.addReference(248, "&oslash;");
        html5References.addReference(248, "&oslash");
        html5References.addReference(249, "&ugrave;");
        html5References.addReference(249, "&ugrave");
        html5References.addReference(250, "&uacute;");
        html5References.addReference(250, "&uacute");
        html5References.addReference(251, "&ucirc;");
        html5References.addReference(251, "&ucirc");
        html5References.addReference(252, "&uuml;");
        html5References.addReference(252, "&uuml");
        html5References.addReference(253, "&yacute;");
        html5References.addReference(253, "&yacute");
        html5References.addReference(254, "&thorn;");
        html5References.addReference(254, "&thorn");
        html5References.addReference(255, "&yuml;");
        html5References.addReference(255, "&yuml");
        html5References.addReference(256, "&Amacr;");
        html5References.addReference(257, "&amacr;");
        html5References.addReference(258, "&Abreve;");
        html5References.addReference(259, "&abreve;");
        html5References.addReference(260, "&Aogon;");
        html5References.addReference(261, "&aogon;");
        html5References.addReference(262, "&Cacute;");
        html5References.addReference(263, "&cacute;");
        html5References.addReference(264, "&Ccirc;");
        html5References.addReference(265, "&ccirc;");
        html5References.addReference(266, "&Cdot;");
        html5References.addReference(267, "&cdot;");
        html5References.addReference(268, "&Ccaron;");
        html5References.addReference(269, "&ccaron;");
        html5References.addReference(270, "&Dcaron;");
        html5References.addReference(271, "&dcaron;");
        html5References.addReference(272, "&Dstrok;");
        html5References.addReference(273, "&dstrok;");
        html5References.addReference(274, "&Emacr;");
        html5References.addReference(275, "&emacr;");
        html5References.addReference(278, "&Edot;");
        html5References.addReference(279, "&edot;");
        html5References.addReference(280, "&Eogon;");
        html5References.addReference(281, "&eogon;");
        html5References.addReference(282, "&Ecaron;");
        html5References.addReference(283, "&ecaron;");
        html5References.addReference(284, "&Gcirc;");
        html5References.addReference(285, "&gcirc;");
        html5References.addReference(286, "&Gbreve;");
        html5References.addReference(287, "&gbreve;");
        html5References.addReference(288, "&Gdot;");
        html5References.addReference(289, "&gdot;");
        html5References.addReference(290, "&Gcedil;");
        html5References.addReference(292, "&Hcirc;");
        html5References.addReference(293, "&hcirc;");
        html5References.addReference(294, "&Hstrok;");
        html5References.addReference(295, "&hstrok;");
        html5References.addReference(296, "&Itilde;");
        html5References.addReference(297, "&itilde;");
        html5References.addReference(298, "&Imacr;");
        html5References.addReference(299, "&imacr;");
        html5References.addReference(302, "&Iogon;");
        html5References.addReference(303, "&iogon;");
        html5References.addReference(304, "&Idot;");
        html5References.addReference(305, "&imath;");
        html5References.addReference(305, "&inodot;");
        html5References.addReference(306, "&IJlig;");
        html5References.addReference(307, "&ijlig;");
        html5References.addReference(308, "&Jcirc;");
        html5References.addReference(309, "&jcirc;");
        html5References.addReference(310, "&Kcedil;");
        html5References.addReference(311, "&kcedil;");
        html5References.addReference(312, "&kgreen;");
        html5References.addReference(313, "&Lacute;");
        html5References.addReference(314, "&lacute;");
        html5References.addReference(315, "&Lcedil;");
        html5References.addReference(316, "&lcedil;");
        html5References.addReference(317, "&Lcaron;");
        html5References.addReference(318, "&lcaron;");
        html5References.addReference(319, "&Lmidot;");
        html5References.addReference(320, "&lmidot;");
        html5References.addReference(321, "&Lstrok;");
        html5References.addReference(322, "&lstrok;");
        html5References.addReference(323, "&Nacute;");
        html5References.addReference(324, "&nacute;");
        html5References.addReference(325, "&Ncedil;");
        html5References.addReference(326, "&ncedil;");
        html5References.addReference(327, "&Ncaron;");
        html5References.addReference(328, "&ncaron;");
        html5References.addReference(329, "&napos;");
        html5References.addReference(330, "&ENG;");
        html5References.addReference(331, "&eng;");
        html5References.addReference(332, "&Omacr;");
        html5References.addReference(333, "&omacr;");
        html5References.addReference(336, "&Odblac;");
        html5References.addReference(337, "&odblac;");
        html5References.addReference(338, "&OElig;");
        html5References.addReference(339, "&oelig;");
        html5References.addReference(340, "&Racute;");
        html5References.addReference(341, "&racute;");
        html5References.addReference(342, "&Rcedil;");
        html5References.addReference(343, "&rcedil;");
        html5References.addReference(344, "&Rcaron;");
        html5References.addReference(345, "&rcaron;");
        html5References.addReference(346, "&Sacute;");
        html5References.addReference(347, "&sacute;");
        html5References.addReference(348, "&Scirc;");
        html5References.addReference(349, "&scirc;");
        html5References.addReference(350, "&Scedil;");
        html5References.addReference(351, "&scedil;");
        html5References.addReference(352, "&Scaron;");
        html5References.addReference(353, "&scaron;");
        html5References.addReference(354, "&Tcedil;");
        html5References.addReference(355, "&tcedil;");
        html5References.addReference(356, "&Tcaron;");
        html5References.addReference(357, "&tcaron;");
        html5References.addReference(358, "&Tstrok;");
        html5References.addReference(359, "&tstrok;");
        html5References.addReference(360, "&Utilde;");
        html5References.addReference(361, "&utilde;");
        html5References.addReference(362, "&Umacr;");
        html5References.addReference(363, "&umacr;");
        html5References.addReference(364, "&Ubreve;");
        html5References.addReference(365, "&ubreve;");
        html5References.addReference(366, "&Uring;");
        html5References.addReference(367, "&uring;");
        html5References.addReference(368, "&Udblac;");
        html5References.addReference(369, "&udblac;");
        html5References.addReference(370, "&Uogon;");
        html5References.addReference(371, "&uogon;");
        html5References.addReference(372, "&Wcirc;");
        html5References.addReference(373, "&wcirc;");
        html5References.addReference(374, "&Ycirc;");
        html5References.addReference(375, "&ycirc;");
        html5References.addReference(376, "&Yuml;");
        html5References.addReference(377, "&Zacute;");
        html5References.addReference(378, "&zacute;");
        html5References.addReference(379, "&Zdot;");
        html5References.addReference(380, "&zdot;");
        html5References.addReference(381, "&Zcaron;");
        html5References.addReference(382, "&zcaron;");
        html5References.addReference(402, "&fnof;");
        html5References.addReference(437, "&imped;");
        html5References.addReference(501, "&gacute;");
        html5References.addReference(567, "&jmath;");
        html5References.addReference(710, "&circ;");
        html5References.addReference(711, "&caron;");
        html5References.addReference(711, "&Hacek;");
        html5References.addReference(728, "&breve;");
        html5References.addReference(728, "&Breve;");
        html5References.addReference(729, "&dot;");
        html5References.addReference(729, "&DiacriticalDot;");
        html5References.addReference(730, "&ring;");
        html5References.addReference(731, "&ogon;");
        html5References.addReference(732, "&tilde;");
        html5References.addReference(732, "&DiacriticalTilde;");
        html5References.addReference(733, "&dblac;");
        html5References.addReference(733, "&DiacriticalDoubleAcute;");
        html5References.addReference(785, "&DownBreve;");
        html5References.addReference(913, "&Alpha;");
        html5References.addReference(914, "&Beta;");
        html5References.addReference(915, "&Gamma;");
        html5References.addReference(916, "&Delta;");
        html5References.addReference(917, "&Epsilon;");
        html5References.addReference(918, "&Zeta;");
        html5References.addReference(919, "&Eta;");
        html5References.addReference(920, "&Theta;");
        html5References.addReference(921, "&Iota;");
        html5References.addReference(922, "&Kappa;");
        html5References.addReference(923, "&Lambda;");
        html5References.addReference(924, "&Mu;");
        html5References.addReference(925, "&Nu;");
        html5References.addReference(926, "&Xi;");
        html5References.addReference(927, "&Omicron;");
        html5References.addReference(928, "&Pi;");
        html5References.addReference(929, "&Rho;");
        html5References.addReference(931, "&Sigma;");
        html5References.addReference(932, "&Tau;");
        html5References.addReference(933, "&Upsilon;");
        html5References.addReference(934, "&Phi;");
        html5References.addReference(935, "&Chi;");
        html5References.addReference(936, "&Psi;");
        html5References.addReference(937, "&Omega;");
        html5References.addReference(937, "&ohm;");
        html5References.addReference(945, "&alpha;");
        html5References.addReference(946, "&beta;");
        html5References.addReference(947, "&gamma;");
        html5References.addReference(948, "&delta;");
        html5References.addReference(949, "&epsilon;");
        html5References.addReference(949, "&epsi;");
        html5References.addReference(950, "&zeta;");
        html5References.addReference(951, "&eta;");
        html5References.addReference(952, "&theta;");
        html5References.addReference(953, "&iota;");
        html5References.addReference(954, "&kappa;");
        html5References.addReference(955, "&lambda;");
        html5References.addReference(956, "&mu;");
        html5References.addReference(957, "&nu;");
        html5References.addReference(958, "&xi;");
        html5References.addReference(959, "&omicron;");
        html5References.addReference(960, "&pi;");
        html5References.addReference(961, "&rho;");
        html5References.addReference(962, "&sigmaf;");
        html5References.addReference(962, "&sigmav;");
        html5References.addReference(962, "&varsigma;");
        html5References.addReference(963, "&sigma;");
        html5References.addReference(964, "&tau;");
        html5References.addReference(965, "&upsilon;");
        html5References.addReference(965, "&upsi;");
        html5References.addReference(966, "&phi;");
        html5References.addReference(967, "&chi;");
        html5References.addReference(968, "&psi;");
        html5References.addReference(969, "&omega;");
        html5References.addReference(977, "&thetasym;");
        html5References.addReference(977, "&thetav;");
        html5References.addReference(977, "&vartheta;");
        html5References.addReference(978, "&upsih;");
        html5References.addReference(978, "&Upsi;");
        html5References.addReference(981, "&phiv;");
        html5References.addReference(981, "&straightphi;");
        html5References.addReference(981, "&varphi;");
        html5References.addReference(982, "&piv;");
        html5References.addReference(982, "&varpi;");
        html5References.addReference(988, "&Gammad;");
        html5References.addReference(989, "&digamma;");
        html5References.addReference(989, "&gammad;");
        html5References.addReference(1008, "&kappav;");
        html5References.addReference(1008, "&varkappa;");
        html5References.addReference(1009, "&rhov;");
        html5References.addReference(1009, "&varrho;");
        html5References.addReference(1013, "&epsiv;");
        html5References.addReference(1013, "&straightepsilon;");
        html5References.addReference(1013, "&varepsilon;");
        html5References.addReference(1014, "&backepsilon;");
        html5References.addReference(1014, "&bepsi;");
        html5References.addReference(1025, "&IOcy;");
        html5References.addReference(1026, "&DJcy;");
        html5References.addReference(1027, "&GJcy;");
        html5References.addReference(1028, "&Jukcy;");
        html5References.addReference(1029, "&DScy;");
        html5References.addReference(1030, "&Iukcy;");
        html5References.addReference(1031, "&YIcy;");
        html5References.addReference(1032, "&Jsercy;");
        html5References.addReference(1033, "&LJcy;");
        html5References.addReference(1034, "&NJcy;");
        html5References.addReference(1035, "&TSHcy;");
        html5References.addReference(1036, "&KJcy;");
        html5References.addReference(1038, "&Ubrcy;");
        html5References.addReference(1039, "&DZcy;");
        html5References.addReference(1040, "&Acy;");
        html5References.addReference(1041, "&Bcy;");
        html5References.addReference(1042, "&Vcy;");
        html5References.addReference(1043, "&Gcy;");
        html5References.addReference(1044, "&Dcy;");
        html5References.addReference(1045, "&IEcy;");
        html5References.addReference(1046, "&ZHcy;");
        html5References.addReference(1047, "&Zcy;");
        html5References.addReference(1048, "&Icy;");
        html5References.addReference(1049, "&Jcy;");
        html5References.addReference(1050, "&Kcy;");
        html5References.addReference(1051, "&Lcy;");
        html5References.addReference(1052, "&Mcy;");
        html5References.addReference(1053, "&Ncy;");
        html5References.addReference(1054, "&Ocy;");
        html5References.addReference(1055, "&Pcy;");
        html5References.addReference(1056, "&Rcy;");
        html5References.addReference(1057, "&Scy;");
        html5References.addReference(1058, "&Tcy;");
        html5References.addReference(1059, "&Ucy;");
        html5References.addReference(1060, "&Fcy;");
        html5References.addReference(1061, "&KHcy;");
        html5References.addReference(1062, "&TScy;");
        html5References.addReference(1063, "&CHcy;");
        html5References.addReference(1064, "&SHcy;");
        html5References.addReference(1065, "&SHCHcy;");
        html5References.addReference(1066, "&HARDcy;");
        html5References.addReference(1067, "&Ycy;");
        html5References.addReference(1068, "&SOFTcy;");
        html5References.addReference(1069, "&Ecy;");
        html5References.addReference(1070, "&YUcy;");
        html5References.addReference(1071, "&YAcy;");
        html5References.addReference(1072, "&acy;");
        html5References.addReference(1073, "&bcy;");
        html5References.addReference(1074, "&vcy;");
        html5References.addReference(1075, "&gcy;");
        html5References.addReference(1076, "&dcy;");
        html5References.addReference(1077, "&iecy;");
        html5References.addReference(1078, "&zhcy;");
        html5References.addReference(1079, "&zcy;");
        html5References.addReference(1080, "&icy;");
        html5References.addReference(1081, "&jcy;");
        html5References.addReference(1082, "&kcy;");
        html5References.addReference(1083, "&lcy;");
        html5References.addReference(1084, "&mcy;");
        html5References.addReference(1085, "&ncy;");
        html5References.addReference(1086, "&ocy;");
        html5References.addReference(1087, "&pcy;");
        html5References.addReference(1088, "&rcy;");
        html5References.addReference(1089, "&scy;");
        html5References.addReference(1090, "&tcy;");
        html5References.addReference(1091, "&ucy;");
        html5References.addReference(1092, "&fcy;");
        html5References.addReference(1093, "&khcy;");
        html5References.addReference(1094, "&tscy;");
        html5References.addReference(1095, "&chcy;");
        html5References.addReference(1096, "&shcy;");
        html5References.addReference(1097, "&shchcy;");
        html5References.addReference(1098, "&hardcy;");
        html5References.addReference(1099, "&ycy;");
        html5References.addReference(1100, "&softcy;");
        html5References.addReference(1101, "&ecy;");
        html5References.addReference(1102, "&yucy;");
        html5References.addReference(1103, "&yacy;");
        html5References.addReference(1105, "&iocy;");
        html5References.addReference(1106, "&djcy;");
        html5References.addReference(1107, "&gjcy;");
        html5References.addReference(1108, "&jukcy;");
        html5References.addReference(1109, "&dscy;");
        html5References.addReference(1110, "&iukcy;");
        html5References.addReference(1111, "&yicy;");
        html5References.addReference(1112, "&jsercy;");
        html5References.addReference(1113, "&ljcy;");
        html5References.addReference(1114, "&njcy;");
        html5References.addReference(1115, "&tshcy;");
        html5References.addReference(1116, "&kjcy;");
        html5References.addReference(1118, "&ubrcy;");
        html5References.addReference(1119, "&dzcy;");
        html5References.addReference(8194, "&ensp;");
        html5References.addReference(8195, "&emsp;");
        html5References.addReference(8196, "&emsp13;");
        html5References.addReference(8197, "&emsp14;");
        html5References.addReference(8199, "&numsp;");
        html5References.addReference(8200, "&puncsp;");
        html5References.addReference(8201, "&thinsp;");
        html5References.addReference(8201, "&ThinSpace;");
        html5References.addReference(8202, "&hairsp;");
        html5References.addReference(8202, "&VeryThinSpace;");
        html5References.addReference(8203, "&NegativeMediumSpace;");
        html5References.addReference(8203, "&NegativeThickSpace;");
        html5References.addReference(8203, "&NegativeThinSpace;");
        html5References.addReference(8203, "&NegativeVeryThinSpace;");
        html5References.addReference(8203, "&ZeroWidthSpace;");
        html5References.addReference(8204, "&zwnj;");
        html5References.addReference(8205, "&zwj;");
        html5References.addReference(8206, "&lrm;");
        html5References.addReference(8207, "&rlm;");
        html5References.addReference(8208, "&dash;");
        html5References.addReference(8208, "&hyphen;");
        html5References.addReference(8211, "&ndash;");
        html5References.addReference(8212, "&mdash;");
        html5References.addReference(8213, "&horbar;");
        html5References.addReference(8214, "&Verbar;");
        html5References.addReference(8214, "&Vert;");
        html5References.addReference(8216, "&lsquo;");
        html5References.addReference(8216, "&OpenCurlyQuote;");
        html5References.addReference(8217, "&rsquo;");
        html5References.addReference(8217, "&rsquor;");
        html5References.addReference(8217, "&CloseCurlyQuote;");
        html5References.addReference(8218, "&sbquo;");
        html5References.addReference(8218, "&lsquor;");
        html5References.addReference(8220, "&ldquo;");
        html5References.addReference(8220, "&OpenCurlyDoubleQuote;");
        html5References.addReference(8221, "&rdquo;");
        html5References.addReference(8221, "&rdquor;");
        html5References.addReference(8221, "&CloseCurlyDoubleQuote;");
        html5References.addReference(8222, "&bdquo;");
        html5References.addReference(8222, "&ldquor;");
        html5References.addReference(8224, "&dagger;");
        html5References.addReference(8225, "&Dagger;");
        html5References.addReference(8225, "&ddagger;");
        html5References.addReference(8226, "&bull;");
        html5References.addReference(8226, "&bullet;");
        html5References.addReference(8229, "&nldr;");
        html5References.addReference(8230, "&hellip;");
        html5References.addReference(8230, "&mldr;");
        html5References.addReference(8240, "&permil;");
        html5References.addReference(8241, "&pertenk;");
        html5References.addReference(8242, "&prime;");
        html5References.addReference(8243, "&Prime;");
        html5References.addReference(8244, "&tprime;");
        html5References.addReference(8245, "&backprime;");
        html5References.addReference(8245, "&bprime;");
        html5References.addReference(8249, "&lsaquo;");
        html5References.addReference(8250, "&rsaquo;");
        html5References.addReference(8254, "&oline;");
        html5References.addReference(8254, "&OverBar;");
        html5References.addReference(8257, "&caret;");
        html5References.addReference(8259, "&hybull;");
        html5References.addReference(8260, "&frasl;");
        html5References.addReference(8271, "&bsemi;");
        html5References.addReference(8279, "&qprime;");
        html5References.addReference(8287, "&MediumSpace;");
        html5References.addReference(8287, 8202, "&ThickSpace;");
        html5References.addReference(8288, "&NoBreak;");
        html5References.addReference(8289, "&af;");
        html5References.addReference(8289, "&ApplyFunction;");
        html5References.addReference(8290, "&it;");
        html5References.addReference(8290, "&InvisibleTimes;");
        html5References.addReference(8291, "&ic;");
        html5References.addReference(8291, "&InvisibleComma;");
        html5References.addReference(8364, "&euro;");
        html5References.addReference(8411, "&tdot;");
        html5References.addReference(8411, "&TripleDot;");
        html5References.addReference(8412, "&DotDot;");
        html5References.addReference(8450, "&complexes;");
        html5References.addReference(8450, "&Copf;");
        html5References.addReference(8453, "&incare;");
        html5References.addReference(8458, "&gscr;");
        html5References.addReference(8459, "&hamilt;");
        html5References.addReference(8459, "&HilbertSpace;");
        html5References.addReference(8459, "&Hscr;");
        html5References.addReference(8460, "&Hfr;");
        html5References.addReference(8460, "&Poincareplane;");
        html5References.addReference(8461, "&quaternions;");
        html5References.addReference(8461, "&Hopf;");
        html5References.addReference(8462, "&planckh;");
        html5References.addReference(8463, "&hbar;");
        html5References.addReference(8463, "&hslash;");
        html5References.addReference(8463, "&planck;");
        html5References.addReference(8463, "&plankv;");
        html5References.addReference(8464, "&imagline;");
        html5References.addReference(8464, "&Iscr;");
        html5References.addReference(8465, "&image;");
        html5References.addReference(8465, "&imagpart;");
        html5References.addReference(8465, "&Ifr;");
        html5References.addReference(8465, "&Im;");
        html5References.addReference(8466, "&lagran;");
        html5References.addReference(8466, "&Laplacetrf;");
        html5References.addReference(8466, "&Lscr;");
        html5References.addReference(8467, "&ell;");
        html5References.addReference(8469, "&naturals;");
        html5References.addReference(8469, "&Nopf;");
        html5References.addReference(8470, "&numero;");
        html5References.addReference(8471, "&copysr;");
        html5References.addReference(8472, "&weierp;");
        html5References.addReference(8472, "&wp;");
        html5References.addReference(8473, "&primes;");
        html5References.addReference(8473, "&Popf;");
        html5References.addReference(8474, "&rationals;");
        html5References.addReference(8474, "&Qopf;");
        html5References.addReference(8475, "&realine;");
        html5References.addReference(8475, "&Rscr;");
        html5References.addReference(8476, "&real;");
        html5References.addReference(8476, "&realpart;");
        html5References.addReference(8476, "&Re;");
        html5References.addReference(8476, "&Rfr;");
        html5References.addReference(8477, "&reals;");
        html5References.addReference(8477, "&Ropf;");
        html5References.addReference(8478, "&rx;");
        html5References.addReference(8482, "&trade;");
        html5References.addReference(8482, "&TRADE;");
        html5References.addReference(8484, "&integers;");
        html5References.addReference(8484, "&Zopf;");
        html5References.addReference(8487, "&mho;");
        html5References.addReference(8488, "&zeetrf;");
        html5References.addReference(8488, "&Zfr;");
        html5References.addReference(8489, "&iiota;");
        html5References.addReference(8492, "&bernou;");
        html5References.addReference(8492, "&Bernoullis;");
        html5References.addReference(8492, "&Bscr;");
        html5References.addReference(8493, "&Cayleys;");
        html5References.addReference(8493, "&Cfr;");
        html5References.addReference(8495, "&escr;");
        html5References.addReference(8496, "&expectation;");
        html5References.addReference(8496, "&Escr;");
        html5References.addReference(8497, "&Fouriertrf;");
        html5References.addReference(8497, "&Fscr;");
        html5References.addReference(8499, "&phmmat;");
        html5References.addReference(8499, "&Mellintrf;");
        html5References.addReference(8499, "&Mscr;");
        html5References.addReference(8500, "&order;");
        html5References.addReference(8500, "&orderof;");
        html5References.addReference(8500, "&oscr;");
        html5References.addReference(8501, "&alefsym;");
        html5References.addReference(8501, "&aleph;");
        html5References.addReference(8502, "&beth;");
        html5References.addReference(8503, "&gimel;");
        html5References.addReference(8504, "&daleth;");
        html5References.addReference(8517, "&CapitalDifferentialD;");
        html5References.addReference(8517, "&DD;");
        html5References.addReference(8518, "&dd;");
        html5References.addReference(8518, "&DifferentialD;");
        html5References.addReference(8519, "&ee;");
        html5References.addReference(8519, "&exponentiale;");
        html5References.addReference(8519, "&ExponentialE;");
        html5References.addReference(8520, "&ii;");
        html5References.addReference(8520, "&ImaginaryI;");
        html5References.addReference(8531, "&frac13;");
        html5References.addReference(8532, "&frac23;");
        html5References.addReference(8533, "&frac15;");
        html5References.addReference(8534, "&frac25;");
        html5References.addReference(8535, "&frac35;");
        html5References.addReference(8536, "&frac45;");
        html5References.addReference(8537, "&frac16;");
        html5References.addReference(8538, "&frac56;");
        html5References.addReference(8539, "&frac18;");
        html5References.addReference(8540, "&frac38;");
        html5References.addReference(8541, "&frac58;");
        html5References.addReference(8542, "&frac78;");
        html5References.addReference(8592, "&larr;");
        html5References.addReference(8592, "&leftarrow;");
        html5References.addReference(8592, "&slarr;");
        html5References.addReference(8592, "&LeftArrow;");
        html5References.addReference(8592, "&ShortLeftArrow;");
        html5References.addReference(8593, "&uarr;");
        html5References.addReference(8593, "&uparrow;");
        html5References.addReference(8593, "&ShortUpArrow;");
        html5References.addReference(8593, "&UpArrow;");
        html5References.addReference(8594, "&rarr;");
        html5References.addReference(8594, "&rightarrow;");
        html5References.addReference(8594, "&srarr;");
        html5References.addReference(8594, "&RightArrow;");
        html5References.addReference(8594, "&ShortRightArrow;");
        html5References.addReference(8595, "&darr;");
        html5References.addReference(8595, "&downarrow;");
        html5References.addReference(8595, "&DownArrow;");
        html5References.addReference(8595, "&ShortDownArrow;");
        html5References.addReference(8596, "&harr;");
        html5References.addReference(8596, "&leftrightarrow;");
        html5References.addReference(8596, "&LeftRightArrow;");
        html5References.addReference(8597, "&updownarrow;");
        html5References.addReference(8597, "&varr;");
        html5References.addReference(8597, "&UpDownArrow;");
        html5References.addReference(8598, "&nwarr;");
        html5References.addReference(8598, "&nwarrow;");
        html5References.addReference(8598, "&UpperLeftArrow;");
        html5References.addReference(8599, "&nearr;");
        html5References.addReference(8599, "&nearrow;");
        html5References.addReference(8599, "&UpperRightArrow;");
        html5References.addReference(8600, "&searr;");
        html5References.addReference(8600, "&searrow;");
        html5References.addReference(8600, "&LowerRightArrow;");
        html5References.addReference(8601, "&swarr;");
        html5References.addReference(8601, "&swarrow;");
        html5References.addReference(8601, "&LowerLeftArrow;");
        html5References.addReference(8602, "&nlarr;");
        html5References.addReference(8602, "&nleftarrow;");
        html5References.addReference(8603, "&nrarr;");
        html5References.addReference(8603, "&nrightarrow;");
        html5References.addReference(8605, "&rarrw;");
        html5References.addReference(8605, "&rightsquigarrow;");
        html5References.addReference(8605, 824, "&nrarrw;");
        html5References.addReference(8606, "&twoheadleftarrow;");
        html5References.addReference(8606, "&Larr;");
        html5References.addReference(8607, "&Uarr;");
        html5References.addReference(8608, "&twoheadrightarrow;");
        html5References.addReference(8608, "&Rarr;");
        html5References.addReference(8609, "&Darr;");
        html5References.addReference(8610, "&larrtl;");
        html5References.addReference(8610, "&leftarrowtail;");
        html5References.addReference(8611, "&rarrtl;");
        html5References.addReference(8611, "&rightarrowtail;");
        html5References.addReference(8612, "&mapstoleft;");
        html5References.addReference(8612, "&LeftTeeArrow;");
        html5References.addReference(8613, "&mapstoup;");
        html5References.addReference(8613, "&UpTeeArrow;");
        html5References.addReference(8614, "&map;");
        html5References.addReference(8614, "&mapsto;");
        html5References.addReference(8614, "&RightTeeArrow;");
        html5References.addReference(8615, "&mapstodown;");
        html5References.addReference(8615, "&DownTeeArrow;");
        html5References.addReference(8617, "&hookleftarrow;");
        html5References.addReference(8617, "&larrhk;");
        html5References.addReference(8618, "&hookrightarrow;");
        html5References.addReference(8618, "&rarrhk;");
        html5References.addReference(8619, "&larrlp;");
        html5References.addReference(8619, "&looparrowleft;");
        html5References.addReference(8620, "&looparrowright;");
        html5References.addReference(8620, "&rarrlp;");
        html5References.addReference(8621, "&harrw;");
        html5References.addReference(8621, "&leftrightsquigarrow;");
        html5References.addReference(8622, "&nharr;");
        html5References.addReference(8622, "&nleftrightarrow;");
        html5References.addReference(8624, "&lsh;");
        html5References.addReference(8624, "&Lsh;");
        html5References.addReference(8625, "&rsh;");
        html5References.addReference(8625, "&Rsh;");
        html5References.addReference(8626, "&ldsh;");
        html5References.addReference(8627, "&rdsh;");
        html5References.addReference(8629, "&crarr;");
        html5References.addReference(8630, "&cularr;");
        html5References.addReference(8630, "&curvearrowleft;");
        html5References.addReference(8631, "&curarr;");
        html5References.addReference(8631, "&curvearrowright;");
        html5References.addReference(8634, "&circlearrowleft;");
        html5References.addReference(8634, "&olarr;");
        html5References.addReference(8635, "&circlearrowright;");
        html5References.addReference(8635, "&orarr;");
        html5References.addReference(8636, "&leftharpoonup;");
        html5References.addReference(8636, "&lharu;");
        html5References.addReference(8636, "&LeftVector;");
        html5References.addReference(8637, "&leftharpoondown;");
        html5References.addReference(8637, "&lhard;");
        html5References.addReference(8637, "&DownLeftVector;");
        html5References.addReference(8638, "&uharr;");
        html5References.addReference(8638, "&upharpoonright;");
        html5References.addReference(8638, "&RightUpVector;");
        html5References.addReference(8639, "&uharl;");
        html5References.addReference(8639, "&upharpoonleft;");
        html5References.addReference(8639, "&LeftUpVector;");
        html5References.addReference(8640, "&rharu;");
        html5References.addReference(8640, "&rightharpoonup;");
        html5References.addReference(8640, "&RightVector;");
        html5References.addReference(8641, "&rhard;");
        html5References.addReference(8641, "&rightharpoondown;");
        html5References.addReference(8641, "&DownRightVector;");
        html5References.addReference(8642, "&dharr;");
        html5References.addReference(8642, "&downharpoonright;");
        html5References.addReference(8642, "&RightDownVector;");
        html5References.addReference(8643, "&dharl;");
        html5References.addReference(8643, "&downharpoonleft;");
        html5References.addReference(8643, "&LeftDownVector;");
        html5References.addReference(8644, "&rightleftarrows;");
        html5References.addReference(8644, "&rlarr;");
        html5References.addReference(8644, "&RightArrowLeftArrow;");
        html5References.addReference(8645, "&udarr;");
        html5References.addReference(8645, "&UpArrowDownArrow;");
        html5References.addReference(8646, "&leftrightarrows;");
        html5References.addReference(8646, "&lrarr;");
        html5References.addReference(8646, "&LeftArrowRightArrow;");
        html5References.addReference(8647, "&leftleftarrows;");
        html5References.addReference(8647, "&llarr;");
        html5References.addReference(8648, "&upuparrows;");
        html5References.addReference(8648, "&uuarr;");
        html5References.addReference(8649, "&rightrightarrows;");
        html5References.addReference(8649, "&rrarr;");
        html5References.addReference(8650, "&ddarr;");
        html5References.addReference(8650, "&downdownarrows;");
        html5References.addReference(8651, "&leftrightharpoons;");
        html5References.addReference(8651, "&lrhar;");
        html5References.addReference(8651, "&ReverseEquilibrium;");
        html5References.addReference(8652, "&rightleftharpoons;");
        html5References.addReference(8652, "&rlhar;");
        html5References.addReference(8652, "&Equilibrium;");
        html5References.addReference(8653, "&nLeftarrow;");
        html5References.addReference(8653, "&nlArr;");
        html5References.addReference(8654, "&nLeftrightarrow;");
        html5References.addReference(8654, "&nhArr;");
        html5References.addReference(8655, "&nRightarrow;");
        html5References.addReference(8655, "&nrArr;");
        html5References.addReference(8656, "&lArr;");
        html5References.addReference(8656, "&DoubleLeftArrow;");
        html5References.addReference(8656, "&Leftarrow;");
        html5References.addReference(8657, "&uArr;");
        html5References.addReference(8657, "&DoubleUpArrow;");
        html5References.addReference(8657, "&Uparrow;");
        html5References.addReference(8658, "&rArr;");
        html5References.addReference(8658, "&DoubleRightArrow;");
        html5References.addReference(8658, "&Implies;");
        html5References.addReference(8658, "&Rightarrow;");
        html5References.addReference(8659, "&dArr;");
        html5References.addReference(8659, "&DoubleDownArrow;");
        html5References.addReference(8659, "&Downarrow;");
        html5References.addReference(8660, "&hArr;");
        html5References.addReference(8660, "&iff;");
        html5References.addReference(8660, "&DoubleLeftRightArrow;");
        html5References.addReference(8660, "&Leftrightarrow;");
        html5References.addReference(8661, "&vArr;");
        html5References.addReference(8661, "&DoubleUpDownArrow;");
        html5References.addReference(8661, "&Updownarrow;");
        html5References.addReference(8662, "&nwArr;");
        html5References.addReference(8663, "&neArr;");
        html5References.addReference(8664, "&seArr;");
        html5References.addReference(8665, "&swArr;");
        html5References.addReference(8666, "&lAarr;");
        html5References.addReference(8666, "&Lleftarrow;");
        html5References.addReference(8667, "&rAarr;");
        html5References.addReference(8667, "&Rrightarrow;");
        html5References.addReference(8669, "&zigrarr;");
        html5References.addReference(8676, "&larrb;");
        html5References.addReference(8676, "&LeftArrowBar;");
        html5References.addReference(8677, "&rarrb;");
        html5References.addReference(8677, "&RightArrowBar;");
        html5References.addReference(8693, "&duarr;");
        html5References.addReference(8693, "&DownArrowUpArrow;");
        html5References.addReference(8701, "&loarr;");
        html5References.addReference(8702, "&roarr;");
        html5References.addReference(8703, "&hoarr;");
        html5References.addReference(8704, "&forall;");
        html5References.addReference(8704, "&ForAll;");
        html5References.addReference(8705, "&comp;");
        html5References.addReference(8705, "&complement;");
        html5References.addReference(8706, "&part;");
        html5References.addReference(8706, "&PartialD;");
        html5References.addReference(8706, 824, "&npart;");
        html5References.addReference(8707, "&exist;");
        html5References.addReference(8707, "&Exists;");
        html5References.addReference(8708, "&nexist;");
        html5References.addReference(8708, "&nexists;");
        html5References.addReference(8708, "&NotExists;");
        html5References.addReference(8709, "&empty;");
        html5References.addReference(8709, "&emptyset;");
        html5References.addReference(8709, "&emptyv;");
        html5References.addReference(8709, "&varnothing;");
        html5References.addReference(8711, "&nabla;");
        html5References.addReference(8711, "&Del;");
        html5References.addReference(8712, "&isin;");
        html5References.addReference(8712, "&in;");
        html5References.addReference(8712, "&isinv;");
        html5References.addReference(8712, "&Element;");
        html5References.addReference(8713, "&notin;");
        html5References.addReference(8713, "&notinva;");
        html5References.addReference(8713, "&NotElement;");
        html5References.addReference(8715, "&ni;");
        html5References.addReference(8715, "&niv;");
        html5References.addReference(8715, "&ReverseElement;");
        html5References.addReference(8715, "&SuchThat;");
        html5References.addReference(8716, "&notni;");
        html5References.addReference(8716, "&notniva;");
        html5References.addReference(8716, "&NotReverseElement;");
        html5References.addReference(8719, "&prod;");
        html5References.addReference(8719, "&Product;");
        html5References.addReference(8720, "&coprod;");
        html5References.addReference(8720, "&Coproduct;");
        html5References.addReference(8721, "&sum;");
        html5References.addReference(8721, "&Sum;");
        html5References.addReference(8722, "&minus;");
        html5References.addReference(8723, "&mnplus;");
        html5References.addReference(8723, "&mp;");
        html5References.addReference(8723, "&MinusPlus;");
        html5References.addReference(8724, "&dotplus;");
        html5References.addReference(8724, "&plusdo;");
        html5References.addReference(8726, "&setminus;");
        html5References.addReference(8726, "&setmn;");
        html5References.addReference(8726, "&smallsetminus;");
        html5References.addReference(8726, "&ssetmn;");
        html5References.addReference(8726, "&Backslash;");
        html5References.addReference(8727, "&lowast;");
        html5References.addReference(8728, "&compfn;");
        html5References.addReference(8728, "&SmallCircle;");
        html5References.addReference(8730, "&radic;");
        html5References.addReference(8730, "&Sqrt;");
        html5References.addReference(8733, "&prop;");
        html5References.addReference(8733, "&propto;");
        html5References.addReference(8733, "&varpropto;");
        html5References.addReference(8733, "&vprop;");
        html5References.addReference(8733, "&Proportional;");
        html5References.addReference(8734, "&infin;");
        html5References.addReference(8735, "&angrt;");
        html5References.addReference(8736, "&ang;");
        html5References.addReference(8736, "&angle;");
        html5References.addReference(8736, 8402, "&nang;");
        html5References.addReference(8737, "&angmsd;");
        html5References.addReference(8737, "&measuredangle;");
        html5References.addReference(8738, "&angsph;");
        html5References.addReference(8739, "&mid;");
        html5References.addReference(8739, "&shortmid;");
        html5References.addReference(8739, "&smid;");
        html5References.addReference(8739, "&VerticalBar;");
        html5References.addReference(8740, "&nmid;");
        html5References.addReference(8740, "&nshortmid;");
        html5References.addReference(8740, "&nsmid;");
        html5References.addReference(8740, "&NotVerticalBar;");
        html5References.addReference(8741, "&par;");
        html5References.addReference(8741, "&parallel;");
        html5References.addReference(8741, "&shortparallel;");
        html5References.addReference(8741, "&spar;");
        html5References.addReference(8741, "&DoubleVerticalBar;");
        html5References.addReference(8742, "&npar;");
        html5References.addReference(8742, "&nparallel;");
        html5References.addReference(8742, "&nshortparallel;");
        html5References.addReference(8742, "&nspar;");
        html5References.addReference(8742, "&NotDoubleVerticalBar;");
        html5References.addReference(8743, "&and;");
        html5References.addReference(8743, "&wedge;");
        html5References.addReference(8744, "&or;");
        html5References.addReference(8744, "&vee;");
        html5References.addReference(8745, "&cap;");
        html5References.addReference(8745, 65024, "&caps;");
        html5References.addReference(8746, "&cup;");
        html5References.addReference(8746, 65024, "&cups;");
        html5References.addReference(8747, "&int;");
        html5References.addReference(8747, "&Integral;");
        html5References.addReference(8748, "&Int;");
        html5References.addReference(8749, "&iiint;");
        html5References.addReference(8749, "&tint;");
        html5References.addReference(8750, "&conint;");
        html5References.addReference(8750, "&oint;");
        html5References.addReference(8750, "&ContourIntegral;");
        html5References.addReference(8751, "&Conint;");
        html5References.addReference(8751, "&DoubleContourIntegral;");
        html5References.addReference(8752, "&Cconint;");
        html5References.addReference(8753, "&cwint;");
        html5References.addReference(8754, "&cwconint;");
        html5References.addReference(8754, "&ClockwiseContourIntegral;");
        html5References.addReference(8755, "&awconint;");
        html5References.addReference(8755, "&CounterClockwiseContourIntegral;");
        html5References.addReference(8756, "&there4;");
        html5References.addReference(8756, "&therefore;");
        html5References.addReference(8756, "&Therefore;");
        html5References.addReference(8757, "&becaus;");
        html5References.addReference(8757, "&because;");
        html5References.addReference(8757, "&Because;");
        html5References.addReference(8758, "&ratio;");
        html5References.addReference(8759, "&Colon;");
        html5References.addReference(8759, "&Proportion;");
        html5References.addReference(8760, "&dotminus;");
        html5References.addReference(8760, "&minusd;");
        html5References.addReference(8762, "&mDDot;");
        html5References.addReference(8763, "&homtht;");
        html5References.addReference(8764, "&sim;");
        html5References.addReference(8764, "&thicksim;");
        html5References.addReference(8764, "&thksim;");
        html5References.addReference(8764, "&Tilde;");
        html5References.addReference(8764, 8402, "&nvsim;");
        html5References.addReference(8765, "&backsim;");
        html5References.addReference(8765, "&bsim;");
        html5References.addReference(8765, 817, "&race;");
        html5References.addReference(8766, "&ac;");
        html5References.addReference(8766, "&mstpos;");
        html5References.addReference(8766, 819, "&acE;");
        html5References.addReference(8767, "&acd;");
        html5References.addReference(8768, "&wr;");
        html5References.addReference(8768, "&wreath;");
        html5References.addReference(8768, "&VerticalTilde;");
        html5References.addReference(8769, "&nsim;");
        html5References.addReference(8769, "&NotTilde;");
        html5References.addReference(8770, "&eqsim;");
        html5References.addReference(8770, "&esim;");
        html5References.addReference(8770, "&EqualTilde;");
        html5References.addReference(8770, 824, "&nesim;");
        html5References.addReference(8770, 824, "&NotEqualTilde;");
        html5References.addReference(8771, "&sime;");
        html5References.addReference(8771, "&simeq;");
        html5References.addReference(8771, "&TildeEqual;");
        html5References.addReference(8772, "&nsime;");
        html5References.addReference(8772, "&nsimeq;");
        html5References.addReference(8772, "&NotTildeEqual;");
        html5References.addReference(8773, "&cong;");
        html5References.addReference(8773, "&TildeFullEqual;");
        html5References.addReference(8774, "&simne;");
        html5References.addReference(8775, "&ncong;");
        html5References.addReference(8775, "&NotTildeFullEqual;");
        html5References.addReference(8776, "&asymp;");
        html5References.addReference(8776, "&ap;");
        html5References.addReference(8776, "&approx;");
        html5References.addReference(8776, "&thickapprox;");
        html5References.addReference(8776, "&thkap;");
        html5References.addReference(8776, "&TildeTilde;");
        html5References.addReference(8777, "&nap;");
        html5References.addReference(8777, "&napprox;");
        html5References.addReference(8777, "&NotTildeTilde;");
        html5References.addReference(8778, "&ape;");
        html5References.addReference(8778, "&approxeq;");
        html5References.addReference(8779, "&apid;");
        html5References.addReference(8779, 824, "&napid;");
        html5References.addReference(8780, "&backcong;");
        html5References.addReference(8780, "&bcong;");
        html5References.addReference(8781, "&asympeq;");
        html5References.addReference(8781, "&CupCap;");
        html5References.addReference(8781, 8402, "&nvap;");
        html5References.addReference(8782, "&bump;");
        html5References.addReference(8782, "&Bumpeq;");
        html5References.addReference(8782, "&HumpDownHump;");
        html5References.addReference(8782, 824, "&nbump;");
        html5References.addReference(8782, 824, "&NotHumpDownHump;");
        html5References.addReference(8783, "&bumpe;");
        html5References.addReference(8783, "&bumpeq;");
        html5References.addReference(8783, "&HumpEqual;");
        html5References.addReference(8783, 824, "&nbumpe;");
        html5References.addReference(8783, 824, "&NotHumpEqual;");
        html5References.addReference(8784, "&doteq;");
        html5References.addReference(8784, "&esdot;");
        html5References.addReference(8784, "&DotEqual;");
        html5References.addReference(8784, 824, "&nedot;");
        html5References.addReference(8785, "&doteqdot;");
        html5References.addReference(8785, "&eDot;");
        html5References.addReference(8786, "&efDot;");
        html5References.addReference(8786, "&fallingdotseq;");
        html5References.addReference(8787, "&erDot;");
        html5References.addReference(8787, "&risingdotseq;");
        html5References.addReference(8788, "&colone;");
        html5References.addReference(8788, "&coloneq;");
        html5References.addReference(8788, "&Assign;");
        html5References.addReference(8789, "&ecolon;");
        html5References.addReference(8789, "&eqcolon;");
        html5References.addReference(8790, "&ecir;");
        html5References.addReference(8790, "&eqcirc;");
        html5References.addReference(8791, "&circeq;");
        html5References.addReference(8791, "&cire;");
        html5References.addReference(8793, "&wedgeq;");
        html5References.addReference(8794, "&veeeq;");
        html5References.addReference(8796, "&triangleq;");
        html5References.addReference(8796, "&trie;");
        html5References.addReference(8799, "&equest;");
        html5References.addReference(8799, "&questeq;");
        html5References.addReference(8800, "&ne;");
        html5References.addReference(8800, "&NotEqual;");
        html5References.addReference(8801, "&equiv;");
        html5References.addReference(8801, "&Congruent;");
        html5References.addReference(8801, 8421, "&bnequiv;");
        html5References.addReference(8802, "&nequiv;");
        html5References.addReference(8802, "&NotCongruent;");
        html5References.addReference(8804, "&le;");
        html5References.addReference(8804, "&leq;");
        html5References.addReference(8804, 8402, "&nvle;");
        html5References.addReference(8805, "&ge;");
        html5References.addReference(8805, "&geq;");
        html5References.addReference(8805, "&GreaterEqual;");
        html5References.addReference(8805, 8402, "&nvge;");
        html5References.addReference(8806, "&lE;");
        html5References.addReference(8806, "&leqq;");
        html5References.addReference(8806, "&LessFullEqual;");
        html5References.addReference(8806, 824, "&nlE;");
        html5References.addReference(8806, 824, "&nleqq;");
        html5References.addReference(8807, "&gE;");
        html5References.addReference(8807, "&geqq;");
        html5References.addReference(8807, "&GreaterFullEqual;");
        html5References.addReference(8807, 824, "&ngE;");
        html5References.addReference(8807, 824, "&ngeqq;");
        html5References.addReference(8807, 824, "&NotGreaterFullEqual;");
        html5References.addReference(8808, "&lnE;");
        html5References.addReference(8808, "&lneqq;");
        html5References.addReference(8808, 65024, "&lvertneqq;");
        html5References.addReference(8808, 65024, "&lvnE;");
        html5References.addReference(8809, "&gnE;");
        html5References.addReference(8809, "&gneqq;");
        html5References.addReference(8809, 65024, "&gvertneqq;");
        html5References.addReference(8809, 65024, "&gvnE;");
        html5References.addReference(8810, "&ll;");
        html5References.addReference(8810, "&Lt;");
        html5References.addReference(8810, "&NestedLessLess;");
        html5References.addReference(8810, 824, "&nLtv;");
        html5References.addReference(8810, 824, "&NotLessLess;");
        html5References.addReference(8810, 8402, "&nLt;");
        html5References.addReference(8811, "&gg;");
        html5References.addReference(8811, "&Gt;");
        html5References.addReference(8811, "&NestedGreaterGreater;");
        html5References.addReference(8811, 824, "&nGtv;");
        html5References.addReference(8811, 824, "&NotGreaterGreater;");
        html5References.addReference(8811, 8402, "&nGt;");
        html5References.addReference(8812, "&between;");
        html5References.addReference(8812, "&twixt;");
        html5References.addReference(8813, "&NotCupCap;");
        html5References.addReference(8814, "&nless;");
        html5References.addReference(8814, "&nlt;");
        html5References.addReference(8814, "&NotLess;");
        html5References.addReference(8815, "&ngt;");
        html5References.addReference(8815, "&ngtr;");
        html5References.addReference(8815, "&NotGreater;");
        html5References.addReference(8816, "&nle;");
        html5References.addReference(8816, "&nleq;");
        html5References.addReference(8816, "&NotLessEqual;");
        html5References.addReference(8817, "&nge;");
        html5References.addReference(8817, "&ngeq;");
        html5References.addReference(8817, "&NotGreaterEqual;");
        html5References.addReference(8818, "&lesssim;");
        html5References.addReference(8818, "&lsim;");
        html5References.addReference(8818, "&LessTilde;");
        html5References.addReference(8819, "&gsim;");
        html5References.addReference(8819, "&gtrsim;");
        html5References.addReference(8819, "&GreaterTilde;");
        html5References.addReference(8820, "&nlsim;");
        html5References.addReference(8820, "&NotLessTilde;");
        html5References.addReference(8821, "&ngsim;");
        html5References.addReference(8821, "&NotGreaterTilde;");
        html5References.addReference(8822, "&lessgtr;");
        html5References.addReference(8822, "&lg;");
        html5References.addReference(8822, "&LessGreater;");
        html5References.addReference(8823, "&gl;");
        html5References.addReference(8823, "&gtrless;");
        html5References.addReference(8823, "&GreaterLess;");
        html5References.addReference(8824, "&ntlg;");
        html5References.addReference(8824, "&NotLessGreater;");
        html5References.addReference(8825, "&ntgl;");
        html5References.addReference(8825, "&NotGreaterLess;");
        html5References.addReference(8826, "&pr;");
        html5References.addReference(8826, "&prec;");
        html5References.addReference(8826, "&Precedes;");
        html5References.addReference(8827, "&sc;");
        html5References.addReference(8827, "&succ;");
        html5References.addReference(8827, "&Succeeds;");
        html5References.addReference(8828, "&prcue;");
        html5References.addReference(8828, "&preccurlyeq;");
        html5References.addReference(8828, "&PrecedesSlantEqual;");
        html5References.addReference(8829, "&sccue;");
        html5References.addReference(8829, "&succcurlyeq;");
        html5References.addReference(8829, "&SucceedsSlantEqual;");
        html5References.addReference(8830, "&precsim;");
        html5References.addReference(8830, "&prsim;");
        html5References.addReference(8830, "&PrecedesTilde;");
        html5References.addReference(8831, "&scsim;");
        html5References.addReference(8831, "&succsim;");
        html5References.addReference(8831, "&SucceedsTilde;");
        html5References.addReference(8831, 824, "&NotSucceedsTilde;");
        html5References.addReference(8832, "&npr;");
        html5References.addReference(8832, "&nprec;");
        html5References.addReference(8832, "&NotPrecedes;");
        html5References.addReference(8833, "&nsc;");
        html5References.addReference(8833, "&nsucc;");
        html5References.addReference(8833, "&NotSucceeds;");
        html5References.addReference(8834, "&sub;");
        html5References.addReference(8834, "&subset;");
        html5References.addReference(8834, 8402, "&nsubset;");
        html5References.addReference(8834, 8402, "&vnsub;");
        html5References.addReference(8834, 8402, "&NotSubset;");
        html5References.addReference(8835, "&sup;");
        html5References.addReference(8835, "&supset;");
        html5References.addReference(8835, "&Superset;");
        html5References.addReference(8835, 8402, "&nsupset;");
        html5References.addReference(8835, 8402, "&vnsup;");
        html5References.addReference(8835, 8402, "&NotSuperset;");
        html5References.addReference(8836, "&nsub;");
        html5References.addReference(8837, "&nsup;");
        html5References.addReference(8838, "&sube;");
        html5References.addReference(8838, "&subseteq;");
        html5References.addReference(8838, "&SubsetEqual;");
        html5References.addReference(8839, "&supe;");
        html5References.addReference(8839, "&supseteq;");
        html5References.addReference(8839, "&SupersetEqual;");
        html5References.addReference(8840, "&nsube;");
        html5References.addReference(8840, "&nsubseteq;");
        html5References.addReference(8840, "&NotSubsetEqual;");
        html5References.addReference(8841, "&nsupe;");
        html5References.addReference(8841, "&nsupseteq;");
        html5References.addReference(8841, "&NotSupersetEqual;");
        html5References.addReference(8842, "&subne;");
        html5References.addReference(8842, "&subsetneq;");
        html5References.addReference(8842, 65024, "&varsubsetneq;");
        html5References.addReference(8842, 65024, "&vsubne;");
        html5References.addReference(8843, "&supne;");
        html5References.addReference(8843, "&supsetneq;");
        html5References.addReference(8843, 65024, "&varsupsetneq;");
        html5References.addReference(8843, 65024, "&vsupne;");
        html5References.addReference(8845, "&cupdot;");
        html5References.addReference(8846, "&uplus;");
        html5References.addReference(8846, "&UnionPlus;");
        html5References.addReference(8847, "&sqsub;");
        html5References.addReference(8847, "&sqsubset;");
        html5References.addReference(8847, "&SquareSubset;");
        html5References.addReference(8847, 824, "&NotSquareSubset;");
        html5References.addReference(8848, "&sqsup;");
        html5References.addReference(8848, "&sqsupset;");
        html5References.addReference(8848, "&SquareSuperset;");
        html5References.addReference(8848, 824, "&NotSquareSuperset;");
        html5References.addReference(8849, "&sqsube;");
        html5References.addReference(8849, "&sqsubseteq;");
        html5References.addReference(8849, "&SquareSubsetEqual;");
        html5References.addReference(8850, "&sqsupe;");
        html5References.addReference(8850, "&sqsupseteq;");
        html5References.addReference(8850, "&SquareSupersetEqual;");
        html5References.addReference(8851, "&sqcap;");
        html5References.addReference(8851, "&SquareIntersection;");
        html5References.addReference(8851, 65024, "&sqcaps;");
        html5References.addReference(8852, "&sqcup;");
        html5References.addReference(8852, "&SquareUnion;");
        html5References.addReference(8852, 65024, "&sqcups;");
        html5References.addReference(8853, "&oplus;");
        html5References.addReference(8853, "&CirclePlus;");
        html5References.addReference(8854, "&ominus;");
        html5References.addReference(8854, "&CircleMinus;");
        html5References.addReference(8855, "&otimes;");
        html5References.addReference(8855, "&CircleTimes;");
        html5References.addReference(8856, "&osol;");
        html5References.addReference(8857, "&odot;");
        html5References.addReference(8857, "&CircleDot;");
        html5References.addReference(8858, "&circledcirc;");
        html5References.addReference(8858, "&ocir;");
        html5References.addReference(8859, "&circledast;");
        html5References.addReference(8859, "&oast;");
        html5References.addReference(8861, "&circleddash;");
        html5References.addReference(8861, "&odash;");
        html5References.addReference(8862, "&boxplus;");
        html5References.addReference(8862, "&plusb;");
        html5References.addReference(8863, "&boxminus;");
        html5References.addReference(8863, "&minusb;");
        html5References.addReference(8864, "&boxtimes;");
        html5References.addReference(8864, "&timesb;");
        html5References.addReference(8865, "&dotsquare;");
        html5References.addReference(8865, "&sdotb;");
        html5References.addReference(8866, "&vdash;");
        html5References.addReference(8866, "&RightTee;");
        html5References.addReference(8867, "&dashv;");
        html5References.addReference(8867, "&LeftTee;");
        html5References.addReference(8868, "&top;");
        html5References.addReference(8868, "&DownTee;");
        html5References.addReference(8869, "&perp;");
        html5References.addReference(8869, "&bot;");
        html5References.addReference(8869, "&bottom;");
        html5References.addReference(8869, "&UpTee;");
        html5References.addReference(8871, "&models;");
        html5References.addReference(8872, "&vDash;");
        html5References.addReference(8872, "&DoubleRightTee;");
        html5References.addReference(8873, "&Vdash;");
        html5References.addReference(8874, "&Vvdash;");
        html5References.addReference(8875, "&VDash;");
        html5References.addReference(8876, "&nvdash;");
        html5References.addReference(8877, "&nvDash;");
        html5References.addReference(8878, "&nVdash;");
        html5References.addReference(8879, "&nVDash;");
        html5References.addReference(8880, "&prurel;");
        html5References.addReference(8882, "&vartriangleleft;");
        html5References.addReference(8882, "&vltri;");
        html5References.addReference(8882, "&LeftTriangle;");
        html5References.addReference(8883, "&vartriangleright;");
        html5References.addReference(8883, "&vrtri;");
        html5References.addReference(8883, "&RightTriangle;");
        html5References.addReference(8884, "&ltrie;");
        html5References.addReference(8884, "&trianglelefteq;");
        html5References.addReference(8884, "&LeftTriangleEqual;");
        html5References.addReference(8884, 8402, "&nvltrie;");
        html5References.addReference(8885, "&rtrie;");
        html5References.addReference(8885, "&trianglerighteq;");
        html5References.addReference(8885, "&RightTriangleEqual;");
        html5References.addReference(8885, 8402, "&nvrtrie;");
        html5References.addReference(8886, "&origof;");
        html5References.addReference(8887, "&imof;");
        html5References.addReference(8888, "&multimap;");
        html5References.addReference(8888, "&mumap;");
        html5References.addReference(8889, "&hercon;");
        html5References.addReference(8890, "&intcal;");
        html5References.addReference(8890, "&intercal;");
        html5References.addReference(8891, "&veebar;");
        html5References.addReference(8893, "&barvee;");
        html5References.addReference(8894, "&angrtvb;");
        html5References.addReference(8895, "&lrtri;");
        html5References.addReference(8896, "&bigwedge;");
        html5References.addReference(8896, "&xwedge;");
        html5References.addReference(8896, "&Wedge;");
        html5References.addReference(8897, "&bigvee;");
        html5References.addReference(8897, "&xvee;");
        html5References.addReference(8897, "&Vee;");
        html5References.addReference(8898, "&bigcap;");
        html5References.addReference(8898, "&xcap;");
        html5References.addReference(8898, "&Intersection;");
        html5References.addReference(8899, "&bigcup;");
        html5References.addReference(8899, "&xcup;");
        html5References.addReference(8899, "&Union;");
        html5References.addReference(8900, "&diam;");
        html5References.addReference(8900, "&diamond;");
        html5References.addReference(8900, "&Diamond;");
        html5References.addReference(8901, "&sdot;");
        html5References.addReference(8902, "&sstarf;");
        html5References.addReference(8902, "&Star;");
        html5References.addReference(8903, "&divideontimes;");
        html5References.addReference(8903, "&divonx;");
        html5References.addReference(8904, "&bowtie;");
        html5References.addReference(8905, "&ltimes;");
        html5References.addReference(8906, "&rtimes;");
        html5References.addReference(8907, "&leftthreetimes;");
        html5References.addReference(8907, "&lthree;");
        html5References.addReference(8908, "&rightthreetimes;");
        html5References.addReference(8908, "&rthree;");
        html5References.addReference(8909, "&backsimeq;");
        html5References.addReference(8909, "&bsime;");
        html5References.addReference(8910, "&curlyvee;");
        html5References.addReference(8910, "&cuvee;");
        html5References.addReference(8911, "&curlywedge;");
        html5References.addReference(8911, "&cuwed;");
        html5References.addReference(8912, "&Sub;");
        html5References.addReference(8912, "&Subset;");
        html5References.addReference(8913, "&Sup;");
        html5References.addReference(8913, "&Supset;");
        html5References.addReference(8914, "&Cap;");
        html5References.addReference(8915, "&Cup;");
        html5References.addReference(8916, "&fork;");
        html5References.addReference(8916, "&pitchfork;");
        html5References.addReference(8917, "&epar;");
        html5References.addReference(8918, "&lessdot;");
        html5References.addReference(8918, "&ltdot;");
        html5References.addReference(8919, "&gtdot;");
        html5References.addReference(8919, "&gtrdot;");
        html5References.addReference(8920, "&Ll;");
        html5References.addReference(8920, 824, "&nLl;");
        html5References.addReference(8921, "&ggg;");
        html5References.addReference(8921, "&Gg;");
        html5References.addReference(8921, 824, "&nGg;");
        html5References.addReference(8922, "&leg;");
        html5References.addReference(8922, "&lesseqgtr;");
        html5References.addReference(8922, "&LessEqualGreater;");
        html5References.addReference(8922, 65024, "&lesg;");
        html5References.addReference(8923, "&gel;");
        html5References.addReference(8923, "&gtreqless;");
        html5References.addReference(8923, "&GreaterEqualLess;");
        html5References.addReference(8923, 65024, "&gesl;");
        html5References.addReference(8926, "&cuepr;");
        html5References.addReference(8926, "&curlyeqprec;");
        html5References.addReference(8927, "&cuesc;");
        html5References.addReference(8927, "&curlyeqsucc;");
        html5References.addReference(8928, "&nprcue;");
        html5References.addReference(8928, "&NotPrecedesSlantEqual;");
        html5References.addReference(8929, "&nsccue;");
        html5References.addReference(8929, "&NotSucceedsSlantEqual;");
        html5References.addReference(8930, "&nsqsube;");
        html5References.addReference(8930, "&NotSquareSubsetEqual;");
        html5References.addReference(8931, "&nsqsupe;");
        html5References.addReference(8931, "&NotSquareSupersetEqual;");
        html5References.addReference(8934, "&lnsim;");
        html5References.addReference(8935, "&gnsim;");
        html5References.addReference(8936, "&precnsim;");
        html5References.addReference(8936, "&prnsim;");
        html5References.addReference(8937, "&scnsim;");
        html5References.addReference(8937, "&succnsim;");
        html5References.addReference(8938, "&nltri;");
        html5References.addReference(8938, "&ntriangleleft;");
        html5References.addReference(8938, "&NotLeftTriangle;");
        html5References.addReference(8939, "&nrtri;");
        html5References.addReference(8939, "&ntriangleright;");
        html5References.addReference(8939, "&NotRightTriangle;");
        html5References.addReference(8940, "&nltrie;");
        html5References.addReference(8940, "&ntrianglelefteq;");
        html5References.addReference(8940, "&NotLeftTriangleEqual;");
        html5References.addReference(8941, "&nrtrie;");
        html5References.addReference(8941, "&ntrianglerighteq;");
        html5References.addReference(8941, "&NotRightTriangleEqual;");
        html5References.addReference(8942, "&vellip;");
        html5References.addReference(8943, "&ctdot;");
        html5References.addReference(8944, "&utdot;");
        html5References.addReference(8945, "&dtdot;");
        html5References.addReference(8946, "&disin;");
        html5References.addReference(8947, "&isinsv;");
        html5References.addReference(8948, "&isins;");
        html5References.addReference(8949, "&isindot;");
        html5References.addReference(8949, 824, "&notindot;");
        html5References.addReference(8950, "&notinvc;");
        html5References.addReference(8951, "&notinvb;");
        html5References.addReference(8953, "&isinE;");
        html5References.addReference(8953, 824, "&notinE;");
        html5References.addReference(8954, "&nisd;");
        html5References.addReference(8955, "&xnis;");
        html5References.addReference(8956, "&nis;");
        html5References.addReference(8957, "&notnivc;");
        html5References.addReference(8958, "&notnivb;");
        html5References.addReference(8965, "&barwed;");
        html5References.addReference(8965, "&barwedge;");
        html5References.addReference(8966, "&doublebarwedge;");
        html5References.addReference(8966, "&Barwed;");
        html5References.addReference(8968, "&lceil;");
        html5References.addReference(8968, "&LeftCeiling;");
        html5References.addReference(8969, "&rceil;");
        html5References.addReference(8969, "&RightCeiling;");
        html5References.addReference(8970, "&lfloor;");
        html5References.addReference(8970, "&LeftFloor;");
        html5References.addReference(8971, "&rfloor;");
        html5References.addReference(8971, "&RightFloor;");
        html5References.addReference(8972, "&drcrop;");
        html5References.addReference(8973, "&dlcrop;");
        html5References.addReference(8974, "&urcrop;");
        html5References.addReference(8975, "&ulcrop;");
        html5References.addReference(8976, "&bnot;");
        html5References.addReference(8978, "&profline;");
        html5References.addReference(8979, "&profsurf;");
        html5References.addReference(8981, "&telrec;");
        html5References.addReference(8982, "&target;");
        html5References.addReference(8988, "&ulcorn;");
        html5References.addReference(8988, "&ulcorner;");
        html5References.addReference(8989, "&urcorn;");
        html5References.addReference(8989, "&urcorner;");
        html5References.addReference(8990, "&dlcorn;");
        html5References.addReference(8990, "&llcorner;");
        html5References.addReference(8991, "&drcorn;");
        html5References.addReference(8991, "&lrcorner;");
        html5References.addReference(8994, "&frown;");
        html5References.addReference(8994, "&sfrown;");
        html5References.addReference(8995, "&smile;");
        html5References.addReference(8995, "&ssmile;");
        html5References.addReference(9005, "&cylcty;");
        html5References.addReference(9006, "&profalar;");
        html5References.addReference(9014, "&topbot;");
        html5References.addReference(9021, "&ovbar;");
        html5References.addReference(9023, "&solbar;");
        html5References.addReference(9084, "&angzarr;");
        html5References.addReference(9136, "&lmoust;");
        html5References.addReference(9136, "&lmoustache;");
        html5References.addReference(9137, "&rmoust;");
        html5References.addReference(9137, "&rmoustache;");
        html5References.addReference(9140, "&tbrk;");
        html5References.addReference(9140, "&OverBracket;");
        html5References.addReference(9141, "&bbrk;");
        html5References.addReference(9141, "&UnderBracket;");
        html5References.addReference(9142, "&bbrktbrk;");
        html5References.addReference(9180, "&OverParenthesis;");
        html5References.addReference(9181, "&UnderParenthesis;");
        html5References.addReference(9182, "&OverBrace;");
        html5References.addReference(9183, "&UnderBrace;");
        html5References.addReference(9186, "&trpezium;");
        html5References.addReference(9191, "&elinters;");
        html5References.addReference(9251, "&blank;");
        html5References.addReference(9416, "&circledS;");
        html5References.addReference(9416, "&oS;");
        html5References.addReference(9472, "&boxh;");
        html5References.addReference(9472, "&HorizontalLine;");
        html5References.addReference(9474, "&boxv;");
        html5References.addReference(9484, "&boxdr;");
        html5References.addReference(9488, "&boxdl;");
        html5References.addReference(9492, "&boxur;");
        html5References.addReference(9496, "&boxul;");
        html5References.addReference(9500, "&boxvr;");
        html5References.addReference(9508, "&boxvl;");
        html5References.addReference(9516, "&boxhd;");
        html5References.addReference(9524, "&boxhu;");
        html5References.addReference(9532, "&boxvh;");
        html5References.addReference(9552, "&boxH;");
        html5References.addReference(9553, "&boxV;");
        html5References.addReference(9554, "&boxdR;");
        html5References.addReference(9555, "&boxDr;");
        html5References.addReference(9556, "&boxDR;");
        html5References.addReference(9557, "&boxdL;");
        html5References.addReference(9558, "&boxDl;");
        html5References.addReference(9559, "&boxDL;");
        html5References.addReference(9560, "&boxuR;");
        html5References.addReference(9561, "&boxUr;");
        html5References.addReference(9562, "&boxUR;");
        html5References.addReference(9563, "&boxuL;");
        html5References.addReference(9564, "&boxUl;");
        html5References.addReference(9565, "&boxUL;");
        html5References.addReference(9566, "&boxvR;");
        html5References.addReference(9567, "&boxVr;");
        html5References.addReference(9568, "&boxVR;");
        html5References.addReference(9569, "&boxvL;");
        html5References.addReference(9570, "&boxVl;");
        html5References.addReference(9571, "&boxVL;");
        html5References.addReference(9572, "&boxHd;");
        html5References.addReference(9573, "&boxhD;");
        html5References.addReference(9574, "&boxHD;");
        html5References.addReference(9575, "&boxHu;");
        html5References.addReference(9576, "&boxhU;");
        html5References.addReference(9577, "&boxHU;");
        html5References.addReference(9578, "&boxvH;");
        html5References.addReference(9579, "&boxVh;");
        html5References.addReference(9580, "&boxVH;");
        html5References.addReference(9600, "&uhblk;");
        html5References.addReference(9604, "&lhblk;");
        html5References.addReference(9608, "&block;");
        html5References.addReference(9617, "&blk14;");
        html5References.addReference(9618, "&blk12;");
        html5References.addReference(9619, "&blk34;");
        html5References.addReference(9633, "&squ;");
        html5References.addReference(9633, "&square;");
        html5References.addReference(9633, "&Square;");
        html5References.addReference(9642, "&blacksquare;");
        html5References.addReference(9642, "&squarf;");
        html5References.addReference(9642, "&squf;");
        html5References.addReference(9642, "&FilledVerySmallSquare;");
        html5References.addReference(9643, "&EmptyVerySmallSquare;");
        html5References.addReference(9645, "&rect;");
        html5References.addReference(9646, "&marker;");
        html5References.addReference(9649, "&fltns;");
        html5References.addReference(9651, "&bigtriangleup;");
        html5References.addReference(9651, "&xutri;");
        html5References.addReference(9652, "&blacktriangle;");
        html5References.addReference(9652, "&utrif;");
        html5References.addReference(9653, "&triangle;");
        html5References.addReference(9653, "&utri;");
        html5References.addReference(9656, "&blacktriangleright;");
        html5References.addReference(9656, "&rtrif;");
        html5References.addReference(9657, "&rtri;");
        html5References.addReference(9657, "&triangleright;");
        html5References.addReference(9661, "&bigtriangledown;");
        html5References.addReference(9661, "&xdtri;");
        html5References.addReference(9662, "&blacktriangledown;");
        html5References.addReference(9662, "&dtrif;");
        html5References.addReference(9663, "&dtri;");
        html5References.addReference(9663, "&triangledown;");
        html5References.addReference(9666, "&blacktriangleleft;");
        html5References.addReference(9666, "&ltrif;");
        html5References.addReference(9667, "&ltri;");
        html5References.addReference(9667, "&triangleleft;");
        html5References.addReference(9674, "&loz;");
        html5References.addReference(9674, "&lozenge;");
        html5References.addReference(9675, "&cir;");
        html5References.addReference(9708, "&tridot;");
        html5References.addReference(9711, "&bigcirc;");
        html5References.addReference(9711, "&xcirc;");
        html5References.addReference(9720, "&ultri;");
        html5References.addReference(9721, "&urtri;");
        html5References.addReference(9722, "&lltri;");
        html5References.addReference(9723, "&EmptySmallSquare;");
        html5References.addReference(9724, "&FilledSmallSquare;");
        html5References.addReference(9733, "&bigstar;");
        html5References.addReference(9733, "&starf;");
        html5References.addReference(9734, "&star;");
        html5References.addReference(9742, "&phone;");
        html5References.addReference(9792, "&female;");
        html5References.addReference(9794, "&male;");
        html5References.addReference(9824, "&spades;");
        html5References.addReference(9824, "&spadesuit;");
        html5References.addReference(9827, "&clubs;");
        html5References.addReference(9827, "&clubsuit;");
        html5References.addReference(9829, "&hearts;");
        html5References.addReference(9829, "&heartsuit;");
        html5References.addReference(9830, "&diams;");
        html5References.addReference(9830, "&diamondsuit;");
        html5References.addReference(9834, "&sung;");
        html5References.addReference(9837, "&flat;");
        html5References.addReference(9838, "&natur;");
        html5References.addReference(9838, "&natural;");
        html5References.addReference(9839, "&sharp;");
        html5References.addReference(10003, "&check;");
        html5References.addReference(10003, "&checkmark;");
        html5References.addReference(10007, "&cross;");
        html5References.addReference(10016, "&malt;");
        html5References.addReference(10016, "&maltese;");
        html5References.addReference(10038, "&sext;");
        html5References.addReference(10072, "&VerticalSeparator;");
        html5References.addReference(10098, "&lbbrk;");
        html5References.addReference(10099, "&rbbrk;");
        html5References.addReference(10184, "&bsolhsub;");
        html5References.addReference(10185, "&suphsol;");
        html5References.addReference(10214, "&lobrk;");
        html5References.addReference(10214, "&LeftDoubleBracket;");
        html5References.addReference(10215, "&robrk;");
        html5References.addReference(10215, "&RightDoubleBracket;");
        html5References.addReference(10216, "&lang;");
        html5References.addReference(10216, "&langle;");
        html5References.addReference(10216, "&LeftAngleBracket;");
        html5References.addReference(10217, "&rang;");
        html5References.addReference(10217, "&rangle;");
        html5References.addReference(10217, "&RightAngleBracket;");
        html5References.addReference(10218, "&Lang;");
        html5References.addReference(10219, "&Rang;");
        html5References.addReference(10220, "&loang;");
        html5References.addReference(10221, "&roang;");
        html5References.addReference(10229, "&longleftarrow;");
        html5References.addReference(10229, "&xlarr;");
        html5References.addReference(10229, "&LongLeftArrow;");
        html5References.addReference(10230, "&longrightarrow;");
        html5References.addReference(10230, "&xrarr;");
        html5References.addReference(10230, "&LongRightArrow;");
        html5References.addReference(10231, "&longleftrightarrow;");
        html5References.addReference(10231, "&xharr;");
        html5References.addReference(10231, "&LongLeftRightArrow;");
        html5References.addReference(10232, "&xlArr;");
        html5References.addReference(10232, "&DoubleLongLeftArrow;");
        html5References.addReference(10232, "&Longleftarrow;");
        html5References.addReference(10233, "&xrArr;");
        html5References.addReference(10233, "&DoubleLongRightArrow;");
        html5References.addReference(10233, "&Longrightarrow;");
        html5References.addReference(10234, "&xhArr;");
        html5References.addReference(10234, "&DoubleLongLeftRightArrow;");
        html5References.addReference(10234, "&Longleftrightarrow;");
        html5References.addReference(10236, "&longmapsto;");
        html5References.addReference(10236, "&xmap;");
        html5References.addReference(10239, "&dzigrarr;");
        html5References.addReference(10498, "&nvlArr;");
        html5References.addReference(10499, "&nvrArr;");
        html5References.addReference(10500, "&nvHarr;");
        html5References.addReference(10501, "&Map;");
        html5References.addReference(10508, "&lbarr;");
        html5References.addReference(10509, "&bkarow;");
        html5References.addReference(10509, "&rbarr;");
        html5References.addReference(10510, "&lBarr;");
        html5References.addReference(10511, "&dbkarow;");
        html5References.addReference(10511, "&rBarr;");
        html5References.addReference(10512, "&drbkarow;");
        html5References.addReference(10512, "&RBarr;");
        html5References.addReference(10513, "&DDotrahd;");
        html5References.addReference(10514, "&UpArrowBar;");
        html5References.addReference(10515, "&DownArrowBar;");
        html5References.addReference(10518, "&Rarrtl;");
        html5References.addReference(10521, "&latail;");
        html5References.addReference(10522, "&ratail;");
        html5References.addReference(10523, "&lAtail;");
        html5References.addReference(10524, "&rAtail;");
        html5References.addReference(10525, "&larrfs;");
        html5References.addReference(10526, "&rarrfs;");
        html5References.addReference(10527, "&larrbfs;");
        html5References.addReference(10528, "&rarrbfs;");
        html5References.addReference(10531, "&nwarhk;");
        html5References.addReference(10532, "&nearhk;");
        html5References.addReference(10533, "&hksearow;");
        html5References.addReference(10533, "&searhk;");
        html5References.addReference(10534, "&hkswarow;");
        html5References.addReference(10534, "&swarhk;");
        html5References.addReference(10535, "&nwnear;");
        html5References.addReference(10536, "&nesear;");
        html5References.addReference(10536, "&toea;");
        html5References.addReference(10537, "&seswar;");
        html5References.addReference(10537, "&tosa;");
        html5References.addReference(10538, "&swnwar;");
        html5References.addReference(10547, "&rarrc;");
        html5References.addReference(10547, 824, "&nrarrc;");
        html5References.addReference(10549, "&cudarrr;");
        html5References.addReference(10550, "&ldca;");
        html5References.addReference(10551, "&rdca;");
        html5References.addReference(10552, "&cudarrl;");
        html5References.addReference(10553, "&larrpl;");
        html5References.addReference(10556, "&curarrm;");
        html5References.addReference(10557, "&cularrp;");
        html5References.addReference(10565, "&rarrpl;");
        html5References.addReference(10568, "&harrcir;");
        html5References.addReference(10569, "&Uarrocir;");
        html5References.addReference(10570, "&lurdshar;");
        html5References.addReference(10571, "&ldrushar;");
        html5References.addReference(10574, "&LeftRightVector;");
        html5References.addReference(10575, "&RightUpDownVector;");
        html5References.addReference(10576, "&DownLeftRightVector;");
        html5References.addReference(10577, "&LeftUpDownVector;");
        html5References.addReference(10578, "&LeftVectorBar;");
        html5References.addReference(10579, "&RightVectorBar;");
        html5References.addReference(10580, "&RightUpVectorBar;");
        html5References.addReference(10581, "&RightDownVectorBar;");
        html5References.addReference(10582, "&DownLeftVectorBar;");
        html5References.addReference(10583, "&DownRightVectorBar;");
        html5References.addReference(10584, "&LeftUpVectorBar;");
        html5References.addReference(10585, "&LeftDownVectorBar;");
        html5References.addReference(10586, "&LeftTeeVector;");
        html5References.addReference(10587, "&RightTeeVector;");
        html5References.addReference(10588, "&RightUpTeeVector;");
        html5References.addReference(10589, "&RightDownTeeVector;");
        html5References.addReference(10590, "&DownLeftTeeVector;");
        html5References.addReference(10591, "&DownRightTeeVector;");
        html5References.addReference(10592, "&LeftUpTeeVector;");
        html5References.addReference(10593, "&LeftDownTeeVector;");
        html5References.addReference(10594, "&lHar;");
        html5References.addReference(10595, "&uHar;");
        html5References.addReference(10596, "&rHar;");
        html5References.addReference(10597, "&dHar;");
        html5References.addReference(10598, "&luruhar;");
        html5References.addReference(10599, "&ldrdhar;");
        html5References.addReference(10600, "&ruluhar;");
        html5References.addReference(10601, "&rdldhar;");
        html5References.addReference(10602, "&lharul;");
        html5References.addReference(10603, "&llhard;");
        html5References.addReference(10604, "&rharul;");
        html5References.addReference(10605, "&lrhard;");
        html5References.addReference(10606, "&udhar;");
        html5References.addReference(10606, "&UpEquilibrium;");
        html5References.addReference(10607, "&duhar;");
        html5References.addReference(10607, "&ReverseUpEquilibrium;");
        html5References.addReference(10608, "&RoundImplies;");
        html5References.addReference(10609, "&erarr;");
        html5References.addReference(10610, "&simrarr;");
        html5References.addReference(10611, "&larrsim;");
        html5References.addReference(10612, "&rarrsim;");
        html5References.addReference(10613, "&rarrap;");
        html5References.addReference(10614, "&ltlarr;");
        html5References.addReference(10616, "&gtrarr;");
        html5References.addReference(10617, "&subrarr;");
        html5References.addReference(10619, "&suplarr;");
        html5References.addReference(10620, "&lfisht;");
        html5References.addReference(10621, "&rfisht;");
        html5References.addReference(10622, "&ufisht;");
        html5References.addReference(10623, "&dfisht;");
        html5References.addReference(10629, "&lopar;");
        html5References.addReference(10630, "&ropar;");
        html5References.addReference(10635, "&lbrke;");
        html5References.addReference(10636, "&rbrke;");
        html5References.addReference(10637, "&lbrkslu;");
        html5References.addReference(10638, "&rbrksld;");
        html5References.addReference(10639, "&lbrksld;");
        html5References.addReference(10640, "&rbrkslu;");
        html5References.addReference(10641, "&langd;");
        html5References.addReference(10642, "&rangd;");
        html5References.addReference(10643, "&lparlt;");
        html5References.addReference(10644, "&rpargt;");
        html5References.addReference(10645, "&gtlPar;");
        html5References.addReference(10646, "&ltrPar;");
        html5References.addReference(10650, "&vzigzag;");
        html5References.addReference(10652, "&vangrt;");
        html5References.addReference(10653, "&angrtvbd;");
        html5References.addReference(10660, "&ange;");
        html5References.addReference(10661, "&range;");
        html5References.addReference(10662, "&dwangle;");
        html5References.addReference(10663, "&uwangle;");
        html5References.addReference(10664, "&angmsdaa;");
        html5References.addReference(10665, "&angmsdab;");
        html5References.addReference(10666, "&angmsdac;");
        html5References.addReference(10667, "&angmsdad;");
        html5References.addReference(10668, "&angmsdae;");
        html5References.addReference(10669, "&angmsdaf;");
        html5References.addReference(10670, "&angmsdag;");
        html5References.addReference(10671, "&angmsdah;");
        html5References.addReference(10672, "&bemptyv;");
        html5References.addReference(10673, "&demptyv;");
        html5References.addReference(10674, "&cemptyv;");
        html5References.addReference(10675, "&raemptyv;");
        html5References.addReference(10676, "&laemptyv;");
        html5References.addReference(10677, "&ohbar;");
        html5References.addReference(10678, "&omid;");
        html5References.addReference(10679, "&opar;");
        html5References.addReference(10681, "&operp;");
        html5References.addReference(10683, "&olcross;");
        html5References.addReference(10684, "&odsold;");
        html5References.addReference(10686, "&olcir;");
        html5References.addReference(10687, "&ofcir;");
        html5References.addReference(10688, "&olt;");
        html5References.addReference(10689, "&ogt;");
        html5References.addReference(10690, "&cirscir;");
        html5References.addReference(10691, "&cirE;");
        html5References.addReference(10692, "&solb;");
        html5References.addReference(10693, "&bsolb;");
        html5References.addReference(10697, "&boxbox;");
        html5References.addReference(10701, "&trisb;");
        html5References.addReference(10702, "&rtriltri;");
        html5References.addReference(10703, "&LeftTriangleBar;");
        html5References.addReference(10703, 824, "&NotLeftTriangleBar;");
        html5References.addReference(10704, "&RightTriangleBar;");
        html5References.addReference(10704, 824, "&NotRightTriangleBar;");
        html5References.addReference(10716, "&iinfin;");
        html5References.addReference(10717, "&infintie;");
        html5References.addReference(10718, "&nvinfin;");
        html5References.addReference(10723, "&eparsl;");
        html5References.addReference(10724, "&smeparsl;");
        html5References.addReference(10725, "&eqvparsl;");
        html5References.addReference(10731, "&blacklozenge;");
        html5References.addReference(10731, "&lozf;");
        html5References.addReference(10740, "&RuleDelayed;");
        html5References.addReference(10742, "&dsol;");
        html5References.addReference(10752, "&bigodot;");
        html5References.addReference(10752, "&xodot;");
        html5References.addReference(10753, "&bigoplus;");
        html5References.addReference(10753, "&xoplus;");
        html5References.addReference(10754, "&bigotimes;");
        html5References.addReference(10754, "&xotime;");
        html5References.addReference(10756, "&biguplus;");
        html5References.addReference(10756, "&xuplus;");
        html5References.addReference(10758, "&bigsqcup;");
        html5References.addReference(10758, "&xsqcup;");
        html5References.addReference(10764, "&iiiint;");
        html5References.addReference(10764, "&qint;");
        html5References.addReference(10765, "&fpartint;");
        html5References.addReference(10768, "&cirfnint;");
        html5References.addReference(10769, "&awint;");
        html5References.addReference(10770, "&rppolint;");
        html5References.addReference(10771, "&scpolint;");
        html5References.addReference(10772, "&npolint;");
        html5References.addReference(10773, "&pointint;");
        html5References.addReference(10774, "&quatint;");
        html5References.addReference(10775, "&intlarhk;");
        html5References.addReference(10786, "&pluscir;");
        html5References.addReference(10787, "&plusacir;");
        html5References.addReference(10788, "&simplus;");
        html5References.addReference(10789, "&plusdu;");
        html5References.addReference(10790, "&plussim;");
        html5References.addReference(10791, "&plustwo;");
        html5References.addReference(10793, "&mcomma;");
        html5References.addReference(10794, "&minusdu;");
        html5References.addReference(10797, "&loplus;");
        html5References.addReference(10798, "&roplus;");
        html5References.addReference(10799, "&Cross;");
        html5References.addReference(10800, "&timesd;");
        html5References.addReference(10801, "&timesbar;");
        html5References.addReference(10803, "&smashp;");
        html5References.addReference(10804, "&lotimes;");
        html5References.addReference(10805, "&rotimes;");
        html5References.addReference(10806, "&otimesas;");
        html5References.addReference(10807, "&Otimes;");
        html5References.addReference(10808, "&odiv;");
        html5References.addReference(10809, "&triplus;");
        html5References.addReference(10810, "&triminus;");
        html5References.addReference(10811, "&tritime;");
        html5References.addReference(10812, "&intprod;");
        html5References.addReference(10812, "&iprod;");
        html5References.addReference(10815, "&amalg;");
        html5References.addReference(10816, "&capdot;");
        html5References.addReference(10818, "&ncup;");
        html5References.addReference(10819, "&ncap;");
        html5References.addReference(10820, "&capand;");
        html5References.addReference(10821, "&cupor;");
        html5References.addReference(10822, "&cupcap;");
        html5References.addReference(10823, "&capcup;");
        html5References.addReference(10824, "&cupbrcap;");
        html5References.addReference(10825, "&capbrcup;");
        html5References.addReference(10826, "&cupcup;");
        html5References.addReference(10827, "&capcap;");
        html5References.addReference(10828, "&ccups;");
        html5References.addReference(10829, "&ccaps;");
        html5References.addReference(10832, "&ccupssm;");
        html5References.addReference(10835, "&And;");
        html5References.addReference(10836, "&Or;");
        html5References.addReference(10837, "&andand;");
        html5References.addReference(10838, "&oror;");
        html5References.addReference(10839, "&orslope;");
        html5References.addReference(10840, "&andslope;");
        html5References.addReference(10842, "&andv;");
        html5References.addReference(10843, "&orv;");
        html5References.addReference(10844, "&andd;");
        html5References.addReference(10845, "&ord;");
        html5References.addReference(10847, "&wedbar;");
        html5References.addReference(10854, "&sdote;");
        html5References.addReference(10858, "&simdot;");
        html5References.addReference(10861, "&congdot;");
        html5References.addReference(10861, 824, "&ncongdot;");
        html5References.addReference(10862, "&easter;");
        html5References.addReference(10863, "&apacir;");
        html5References.addReference(10864, "&apE;");
        html5References.addReference(10864, 824, "&napE;");
        html5References.addReference(10865, "&eplus;");
        html5References.addReference(10866, "&pluse;");
        html5References.addReference(10867, "&Esim;");
        html5References.addReference(10868, "&Colone;");
        html5References.addReference(10869, "&Equal;");
        html5References.addReference(10871, "&ddotseq;");
        html5References.addReference(10871, "&eDDot;");
        html5References.addReference(10872, "&equivDD;");
        html5References.addReference(10873, "&ltcir;");
        html5References.addReference(10874, "&gtcir;");
        html5References.addReference(10875, "&ltquest;");
        html5References.addReference(10876, "&gtquest;");
        html5References.addReference(10877, "&leqslant;");
        html5References.addReference(10877, "&les;");
        html5References.addReference(10877, "&LessSlantEqual;");
        html5References.addReference(10877, 824, "&nleqslant;");
        html5References.addReference(10877, 824, "&nles;");
        html5References.addReference(10877, 824, "&NotLessSlantEqual;");
        html5References.addReference(10878, "&geqslant;");
        html5References.addReference(10878, "&ges;");
        html5References.addReference(10878, "&GreaterSlantEqual;");
        html5References.addReference(10878, 824, "&ngeqslant;");
        html5References.addReference(10878, 824, "&nges;");
        html5References.addReference(10878, 824, "&NotGreaterSlantEqual;");
        html5References.addReference(10879, "&lesdot;");
        html5References.addReference(10880, "&gesdot;");
        html5References.addReference(10881, "&lesdoto;");
        html5References.addReference(10882, "&gesdoto;");
        html5References.addReference(10883, "&lesdotor;");
        html5References.addReference(10884, "&gesdotol;");
        html5References.addReference(10885, "&lap;");
        html5References.addReference(10885, "&lessapprox;");
        html5References.addReference(10886, "&gap;");
        html5References.addReference(10886, "&gtrapprox;");
        html5References.addReference(10887, "&lne;");
        html5References.addReference(10887, "&lneq;");
        html5References.addReference(10888, "&gne;");
        html5References.addReference(10888, "&gneq;");
        html5References.addReference(10889, "&lnap;");
        html5References.addReference(10889, "&lnapprox;");
        html5References.addReference(10890, "&gnap;");
        html5References.addReference(10890, "&gnapprox;");
        html5References.addReference(10891, "&lEg;");
        html5References.addReference(10891, "&lesseqqgtr;");
        html5References.addReference(10892, "&gEl;");
        html5References.addReference(10892, "&gtreqqless;");
        html5References.addReference(10893, "&lsime;");
        html5References.addReference(10894, "&gsime;");
        html5References.addReference(10895, "&lsimg;");
        html5References.addReference(10896, "&gsiml;");
        html5References.addReference(10897, "&lgE;");
        html5References.addReference(10898, "&glE;");
        html5References.addReference(10899, "&lesges;");
        html5References.addReference(10900, "&gesles;");
        html5References.addReference(10901, "&els;");
        html5References.addReference(10901, "&eqslantless;");
        html5References.addReference(10902, "&egs;");
        html5References.addReference(10902, "&eqslantgtr;");
        html5References.addReference(10903, "&elsdot;");
        html5References.addReference(10904, "&egsdot;");
        html5References.addReference(10905, "&el;");
        html5References.addReference(10906, "&eg;");
        html5References.addReference(10909, "&siml;");
        html5References.addReference(10910, "&simg;");
        html5References.addReference(10911, "&simlE;");
        html5References.addReference(10912, "&simgE;");
        html5References.addReference(10913, "&LessLess;");
        html5References.addReference(10913, 824, "&NotNestedLessLess;");
        html5References.addReference(10914, "&GreaterGreater;");
        html5References.addReference(10914, 824, "&NotNestedGreaterGreater;");
        html5References.addReference(10916, "&glj;");
        html5References.addReference(10917, "&gla;");
        html5References.addReference(10918, "&ltcc;");
        html5References.addReference(10919, "&gtcc;");
        html5References.addReference(10920, "&lescc;");
        html5References.addReference(10921, "&gescc;");
        html5References.addReference(10922, "&smt;");
        html5References.addReference(10923, "&lat;");
        html5References.addReference(10924, "&smte;");
        html5References.addReference(10924, 65024, "&smtes;");
        html5References.addReference(10925, "&late;");
        html5References.addReference(10925, 65024, "&lates;");
        html5References.addReference(10926, "&bumpE;");
        html5References.addReference(10927, "&pre;");
        html5References.addReference(10927, "&preceq;");
        html5References.addReference(10927, "&PrecedesEqual;");
        html5References.addReference(10927, 824, "&npre;");
        html5References.addReference(10927, 824, "&npreceq;");
        html5References.addReference(10927, 824, "&NotPrecedesEqual;");
        html5References.addReference(10928, "&sce;");
        html5References.addReference(10928, "&succeq;");
        html5References.addReference(10928, "&SucceedsEqual;");
        html5References.addReference(10928, 824, "&nsce;");
        html5References.addReference(10928, 824, "&nsucceq;");
        html5References.addReference(10928, 824, "&NotSucceedsEqual;");
        html5References.addReference(10931, "&prE;");
        html5References.addReference(10932, "&scE;");
        html5References.addReference(10933, "&precneqq;");
        html5References.addReference(10933, "&prnE;");
        html5References.addReference(10934, "&scnE;");
        html5References.addReference(10934, "&succneqq;");
        html5References.addReference(10935, "&prap;");
        html5References.addReference(10935, "&precapprox;");
        html5References.addReference(10936, "&scap;");
        html5References.addReference(10936, "&succapprox;");
        html5References.addReference(10937, "&precnapprox;");
        html5References.addReference(10937, "&prnap;");
        html5References.addReference(10938, "&scnap;");
        html5References.addReference(10938, "&succnapprox;");
        html5References.addReference(10939, "&Pr;");
        html5References.addReference(10940, "&Sc;");
        html5References.addReference(10941, "&subdot;");
        html5References.addReference(10942, "&supdot;");
        html5References.addReference(10943, "&subplus;");
        html5References.addReference(10944, "&supplus;");
        html5References.addReference(10945, "&submult;");
        html5References.addReference(10946, "&supmult;");
        html5References.addReference(10947, "&subedot;");
        html5References.addReference(10948, "&supedot;");
        html5References.addReference(10949, "&subE;");
        html5References.addReference(10949, "&subseteqq;");
        html5References.addReference(10949, 824, "&nsubE;");
        html5References.addReference(10949, 824, "&nsubseteqq;");
        html5References.addReference(10950, "&supE;");
        html5References.addReference(10950, "&supseteqq;");
        html5References.addReference(10950, 824, "&nsupE;");
        html5References.addReference(10950, 824, "&nsupseteqq;");
        html5References.addReference(10951, "&subsim;");
        html5References.addReference(10952, "&supsim;");
        html5References.addReference(10955, "&subnE;");
        html5References.addReference(10955, "&subsetneqq;");
        html5References.addReference(10955, 65024, "&varsubsetneqq;");
        html5References.addReference(10955, 65024, "&vsubnE;");
        html5References.addReference(10956, "&supnE;");
        html5References.addReference(10956, "&supsetneqq;");
        html5References.addReference(10956, 65024, "&varsupsetneqq;");
        html5References.addReference(10956, 65024, "&vsupnE;");
        html5References.addReference(10959, "&csub;");
        html5References.addReference(10960, "&csup;");
        html5References.addReference(10961, "&csube;");
        html5References.addReference(10962, "&csupe;");
        html5References.addReference(10963, "&subsup;");
        html5References.addReference(10964, "&supsub;");
        html5References.addReference(10965, "&subsub;");
        html5References.addReference(10966, "&supsup;");
        html5References.addReference(10967, "&suphsub;");
        html5References.addReference(10968, "&supdsub;");
        html5References.addReference(10969, "&forkv;");
        html5References.addReference(10970, "&topfork;");
        html5References.addReference(10971, "&mlcp;");
        html5References.addReference(10980, "&Dashv;");
        html5References.addReference(10980, "&DoubleLeftTee;");
        html5References.addReference(10982, "&Vdashl;");
        html5References.addReference(10983, "&Barv;");
        html5References.addReference(10984, "&vBar;");
        html5References.addReference(10985, "&vBarv;");
        html5References.addReference(10987, "&Vbar;");
        html5References.addReference(10988, "&Not;");
        html5References.addReference(10989, "&bNot;");
        html5References.addReference(10990, "&rnmid;");
        html5References.addReference(10991, "&cirmid;");
        html5References.addReference(10992, "&midcir;");
        html5References.addReference(10993, "&topcir;");
        html5References.addReference(10994, "&nhpar;");
        html5References.addReference(10995, "&parsim;");
        html5References.addReference(11005, "&parsl;");
        html5References.addReference(11005, 8421, "&nparsl;");
        html5References.addReference(64256, "&fflig;");
        html5References.addReference(64257, "&filig;");
        html5References.addReference(64258, "&fllig;");
        html5References.addReference(64259, "&ffilig;");
        html5References.addReference(64260, "&ffllig;");
        html5References.addReference(119964, "&Ascr;");
        html5References.addReference(119966, "&Cscr;");
        html5References.addReference(119967, "&Dscr;");
        html5References.addReference(119970, "&Gscr;");
        html5References.addReference(119973, "&Jscr;");
        html5References.addReference(119974, "&Kscr;");
        html5References.addReference(119977, "&Nscr;");
        html5References.addReference(119978, "&Oscr;");
        html5References.addReference(119979, "&Pscr;");
        html5References.addReference(119980, "&Qscr;");
        html5References.addReference(119982, "&Sscr;");
        html5References.addReference(119983, "&Tscr;");
        html5References.addReference(119984, "&Uscr;");
        html5References.addReference(119985, "&Vscr;");
        html5References.addReference(119986, "&Wscr;");
        html5References.addReference(119987, "&Xscr;");
        html5References.addReference(119988, "&Yscr;");
        html5References.addReference(119989, "&Zscr;");
        html5References.addReference(119990, "&ascr;");
        html5References.addReference(119991, "&bscr;");
        html5References.addReference(119992, "&cscr;");
        html5References.addReference(119993, "&dscr;");
        html5References.addReference(119995, "&fscr;");
        html5References.addReference(119997, "&hscr;");
        html5References.addReference(119998, "&iscr;");
        html5References.addReference(119999, "&jscr;");
        html5References.addReference(120000, "&kscr;");
        html5References.addReference(120001, "&lscr;");
        html5References.addReference(120002, "&mscr;");
        html5References.addReference(120003, "&nscr;");
        html5References.addReference(120005, "&pscr;");
        html5References.addReference(120006, "&qscr;");
        html5References.addReference(120007, "&rscr;");
        html5References.addReference(120008, "&sscr;");
        html5References.addReference(120009, "&tscr;");
        html5References.addReference(120010, "&uscr;");
        html5References.addReference(120011, "&vscr;");
        html5References.addReference(120012, "&wscr;");
        html5References.addReference(120013, "&xscr;");
        html5References.addReference(120014, "&yscr;");
        html5References.addReference(120015, "&zscr;");
        html5References.addReference(120068, "&Afr;");
        html5References.addReference(120069, "&Bfr;");
        html5References.addReference(120071, "&Dfr;");
        html5References.addReference(120072, "&Efr;");
        html5References.addReference(120073, "&Ffr;");
        html5References.addReference(120074, "&Gfr;");
        html5References.addReference(120077, "&Jfr;");
        html5References.addReference(120078, "&Kfr;");
        html5References.addReference(120079, "&Lfr;");
        html5References.addReference(120080, "&Mfr;");
        html5References.addReference(120081, "&Nfr;");
        html5References.addReference(120082, "&Ofr;");
        html5References.addReference(120083, "&Pfr;");
        html5References.addReference(120084, "&Qfr;");
        html5References.addReference(120086, "&Sfr;");
        html5References.addReference(120087, "&Tfr;");
        html5References.addReference(120088, "&Ufr;");
        html5References.addReference(120089, "&Vfr;");
        html5References.addReference(120090, "&Wfr;");
        html5References.addReference(120091, "&Xfr;");
        html5References.addReference(120092, "&Yfr;");
        html5References.addReference(120094, "&afr;");
        html5References.addReference(120095, "&bfr;");
        html5References.addReference(120096, "&cfr;");
        html5References.addReference(120097, "&dfr;");
        html5References.addReference(120098, "&efr;");
        html5References.addReference(120099, "&ffr;");
        html5References.addReference(120100, "&gfr;");
        html5References.addReference(120101, "&hfr;");
        html5References.addReference(120102, "&ifr;");
        html5References.addReference(120103, "&jfr;");
        html5References.addReference(120104, "&kfr;");
        html5References.addReference(120105, "&lfr;");
        html5References.addReference(120106, "&mfr;");
        html5References.addReference(120107, "&nfr;");
        html5References.addReference(120108, "&ofr;");
        html5References.addReference(120109, "&pfr;");
        html5References.addReference(120110, "&qfr;");
        html5References.addReference(120111, "&rfr;");
        html5References.addReference(120112, "&sfr;");
        html5References.addReference(120113, "&tfr;");
        html5References.addReference(120114, "&ufr;");
        html5References.addReference(120115, "&vfr;");
        html5References.addReference(120116, "&wfr;");
        html5References.addReference(120117, "&xfr;");
        html5References.addReference(120118, "&yfr;");
        html5References.addReference(120119, "&zfr;");
        html5References.addReference(120120, "&Aopf;");
        html5References.addReference(120121, "&Bopf;");
        html5References.addReference(120123, "&Dopf;");
        html5References.addReference(120124, "&Eopf;");
        html5References.addReference(120125, "&Fopf;");
        html5References.addReference(120126, "&Gopf;");
        html5References.addReference(120128, "&Iopf;");
        html5References.addReference(120129, "&Jopf;");
        html5References.addReference(120130, "&Kopf;");
        html5References.addReference(120131, "&Lopf;");
        html5References.addReference(120132, "&Mopf;");
        html5References.addReference(120134, "&Oopf;");
        html5References.addReference(120138, "&Sopf;");
        html5References.addReference(120139, "&Topf;");
        html5References.addReference(120140, "&Uopf;");
        html5References.addReference(120141, "&Vopf;");
        html5References.addReference(120142, "&Wopf;");
        html5References.addReference(120143, "&Xopf;");
        html5References.addReference(120144, "&Yopf;");
        html5References.addReference(120146, "&aopf;");
        html5References.addReference(120147, "&bopf;");
        html5References.addReference(120148, "&copf;");
        html5References.addReference(120149, "&dopf;");
        html5References.addReference(120150, "&eopf;");
        html5References.addReference(120151, "&fopf;");
        html5References.addReference(120152, "&gopf;");
        html5References.addReference(120153, "&hopf;");
        html5References.addReference(120154, "&iopf;");
        html5References.addReference(120155, "&jopf;");
        html5References.addReference(120156, "&kopf;");
        html5References.addReference(120157, "&lopf;");
        html5References.addReference(120158, "&mopf;");
        html5References.addReference(120159, "&nopf;");
        html5References.addReference(120160, "&oopf;");
        html5References.addReference(120161, "&popf;");
        html5References.addReference(120162, "&qopf;");
        html5References.addReference(120163, "&ropf;");
        html5References.addReference(120164, "&sopf;");
        html5References.addReference(120165, "&topf;");
        html5References.addReference(120166, "&uopf;");
        html5References.addReference(120167, "&vopf;");
        html5References.addReference(120168, "&wopf;");
        html5References.addReference(120169, "&xopf;");
        html5References.addReference(120170, "&yopf;");
        html5References.addReference(120171, "&zopf;");

        /*
         * Initialization of escape levels.
         * Defined levels :
         *
         *    - Level 0 : Only markup-significant characters except the apostrophe (')
         *    - Level 1 : Only markup-significant characters (including the apostrophe)
         *    - Level 2 : Markup-significant characters plus all non-ASCII
         *    - Level 3 : All non-alphanumeric characters
         *    - Level 4 : All characters
         */
        final byte[] escapeLevels = new byte[0x7f + 2];
        Arrays.fill(escapeLevels, (byte) 3);
        for (char c = 'A'; c <= 'Z'; c++) {
            escapeLevels[c] = 4;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            escapeLevels[c] = 4;
        }
        for (char c = '0'; c <= '9'; c++) {
            escapeLevels[c] = 4;
        }
        escapeLevels['\''] = 1;
        escapeLevels['"'] = 0;
        escapeLevels['<'] = 0;
        escapeLevels['>'] = 0;
        escapeLevels['&'] = 0;
        escapeLevels[0x7f + 1] = 2;


        return new HtmlEscapeSymbols(html5References, escapeLevels);

    }

}

final class Html4EscapeSymbolsInitializer {


    private Html4EscapeSymbolsInitializer() {
        super();
    }

    static HtmlEscapeSymbols initializeHtml4() {

        final HtmlEscapeSymbols.References html4References = new HtmlEscapeSymbols.References();

        /*
         * -----------------------------------------------------------------
         *   HTML4 NAMED CHARACTER REFERENCES (CHARACTER ENTITY REFERENCES)
         *   See: http://www.w3.org/TR/html4/sgml/entities.html
         * -----------------------------------------------------------------
         */

        /* HTML NCRs FOR MARKUP-SIGNIFICANT CHARACTERS */
        // (Note HTML 4 does not include &apos; as a valid NCR)
        html4References.addReference('"', "&quot;");
        html4References.addReference('&', "&amp;");
        html4References.addReference('<', "&lt;");
        html4References.addReference('>', "&gt;");
        /* HTML NCRs FOR ISO-8859-1 CHARACTERS */
        html4References.addReference('\u00A0', "&nbsp;");
        html4References.addReference('\u00A1', "&iexcl;");
        html4References.addReference('\u00A2', "&cent;");
        html4References.addReference('\u00A3', "&pound;");
        html4References.addReference('\u00A4', "&curren;");
        html4References.addReference('\u00A5', "&yen;");
        html4References.addReference('\u00A6', "&brvbar;");
        html4References.addReference('\u00A7', "&sect;");
        html4References.addReference('\u00A8', "&uml;");
        html4References.addReference('\u00A9', "&copy;");
        html4References.addReference('\u00AA', "&ordf;");
        html4References.addReference('\u00AB', "&laquo;");
        html4References.addReference('\u00AC', "&not;");
        html4References.addReference('\u00AD', "&shy;");
        html4References.addReference('\u00AE', "&reg;");
        html4References.addReference('\u00AF', "&macr;");
        html4References.addReference('\u00B0', "&deg;");
        html4References.addReference('\u00B1', "&plusmn;");
        html4References.addReference('\u00B2', "&sup2;");
        html4References.addReference('\u00B3', "&sup3;");
        html4References.addReference('\u00B4', "&acute;");
        html4References.addReference('\u00B5', "&micro;");
        html4References.addReference('\u00B6', "&para;");
        html4References.addReference('\u00B7', "&middot;");
        html4References.addReference('\u00B8', "&cedil;");
        html4References.addReference('\u00B9', "&sup1;");
        html4References.addReference('\u00BA', "&ordm;");
        html4References.addReference('\u00BB', "&raquo;");
        html4References.addReference('\u00BC', "&frac14;");
        html4References.addReference('\u00BD', "&frac12;");
        html4References.addReference('\u00BE', "&frac34;");
        html4References.addReference('\u00BF', "&iquest;");
        html4References.addReference('\u00C0', "&Agrave;");
        html4References.addReference('\u00C1', "&Aacute;");
        html4References.addReference('\u00C2', "&Acirc;");
        html4References.addReference('\u00C3', "&Atilde;");
        html4References.addReference('\u00C4', "&Auml;");
        html4References.addReference('\u00C5', "&Aring;");
        html4References.addReference('\u00C6', "&AElig;");
        html4References.addReference('\u00C7', "&Ccedil;");
        html4References.addReference('\u00C8', "&Egrave;");
        html4References.addReference('\u00C9', "&Eacute;");
        html4References.addReference('\u00CA', "&Ecirc;");
        html4References.addReference('\u00CB', "&Euml;");
        html4References.addReference('\u00CC', "&Igrave;");
        html4References.addReference('\u00CD', "&Iacute;");
        html4References.addReference('\u00CE', "&Icirc;");
        html4References.addReference('\u00CF', "&Iuml;");
        html4References.addReference('\u00D0', "&ETH;");
        html4References.addReference('\u00D1', "&Ntilde;");
        html4References.addReference('\u00D2', "&Ograve;");
        html4References.addReference('\u00D3', "&Oacute;");
        html4References.addReference('\u00D4', "&Ocirc;");
        html4References.addReference('\u00D5', "&Otilde;");
        html4References.addReference('\u00D6', "&Ouml;");
        html4References.addReference('\u00D7', "&times;");
        html4References.addReference('\u00D8', "&Oslash;");
        html4References.addReference('\u00D9', "&Ugrave;");
        html4References.addReference('\u00DA', "&Uacute;");
        html4References.addReference('\u00DB', "&Ucirc;");
        html4References.addReference('\u00DC', "&Uuml;");
        html4References.addReference('\u00DD', "&Yacute;");
        html4References.addReference('\u00DE', "&THORN;");
        html4References.addReference('\u00DF', "&szlig;");
        html4References.addReference('\u00E0', "&agrave;");
        html4References.addReference('\u00E1', "&aacute;");
        html4References.addReference('\u00E2', "&acirc;");
        html4References.addReference('\u00E3', "&atilde;");
        html4References.addReference('\u00E4', "&auml;");
        html4References.addReference('\u00E5', "&aring;");
        html4References.addReference('\u00E6', "&aelig;");
        html4References.addReference('\u00E7', "&ccedil;");
        html4References.addReference('\u00E8', "&egrave;");
        html4References.addReference('\u00E9', "&eacute;");
        html4References.addReference('\u00EA', "&ecirc;");
        html4References.addReference('\u00EB', "&euml;");
        html4References.addReference('\u00EC', "&igrave;");
        html4References.addReference('\u00ED', "&iacute;");
        html4References.addReference('\u00EE', "&icirc;");
        html4References.addReference('\u00EF', "&iuml;");
        html4References.addReference('\u00F0', "&eth;");
        html4References.addReference('\u00F1', "&ntilde;");
        html4References.addReference('\u00F2', "&ograve;");
        html4References.addReference('\u00F3', "&oacute;");
        html4References.addReference('\u00F4', "&ocirc;");
        html4References.addReference('\u00F5', "&otilde;");
        html4References.addReference('\u00F6', "&ouml;");
        html4References.addReference('\u00F7', "&divide;");
        html4References.addReference('\u00F8', "&oslash;");
        html4References.addReference('\u00F9', "&ugrave;");
        html4References.addReference('\u00FA', "&uacute;");
        html4References.addReference('\u00FB', "&ucirc;");
        html4References.addReference('\u00FC', "&uuml;");
        html4References.addReference('\u00FD', "&yacute;");
        html4References.addReference('\u00FE', "&thorn;");
        html4References.addReference('\u00FF', "&yuml;");
        /* HTML NCRs FOR SYMBOLS, MATHEMATICAL SYMBOLS AND GREEK LETTERS */
        /* - Greek */
        html4References.addReference('\u0192', "&fnof;");
        html4References.addReference('\u0391', "&Alpha;");
        html4References.addReference('\u0392', "&Beta;");
        html4References.addReference('\u0393', "&Gamma;");
        html4References.addReference('\u0394', "&Delta;");
        html4References.addReference('\u0395', "&Epsilon;");
        html4References.addReference('\u0396', "&Zeta;");
        html4References.addReference('\u0397', "&Eta;");
        html4References.addReference('\u0398', "&Theta;");
        html4References.addReference('\u0399', "&Iota;");
        html4References.addReference('\u039A', "&Kappa;");
        html4References.addReference('\u039B', "&Lambda;");
        html4References.addReference('\u039C', "&Mu;");
        html4References.addReference('\u039D', "&Nu;");
        html4References.addReference('\u039E', "&Xi;");
        html4References.addReference('\u039F', "&Omicron;");
        html4References.addReference('\u03A0', "&Pi;");
        html4References.addReference('\u03A1', "&Rho;");
        html4References.addReference('\u03A3', "&Sigma;");
        html4References.addReference('\u03A4', "&Tau;");
        html4References.addReference('\u03A5', "&Upsilon;");
        html4References.addReference('\u03A6', "&Phi;");
        html4References.addReference('\u03A7', "&Chi;");
        html4References.addReference('\u03A8', "&Psi;");
        html4References.addReference('\u03A9', "&Omega;");
        html4References.addReference('\u03B1', "&alpha;");
        html4References.addReference('\u03B2', "&beta;");
        html4References.addReference('\u03B3', "&gamma;");
        html4References.addReference('\u03B4', "&delta;");
        html4References.addReference('\u03B5', "&epsilon;");
        html4References.addReference('\u03B6', "&zeta;");
        html4References.addReference('\u03B7', "&eta;");
        html4References.addReference('\u03B8', "&theta;");
        html4References.addReference('\u03B9', "&iota;");
        html4References.addReference('\u03BA', "&kappa;");
        html4References.addReference('\u03BB', "&lambda;");
        html4References.addReference('\u03BC', "&mu;");
        html4References.addReference('\u03BD', "&nu;");
        html4References.addReference('\u03BE', "&xi;");
        html4References.addReference('\u03BF', "&omicron;");
        html4References.addReference('\u03C0', "&pi;");
        html4References.addReference('\u03C1', "&rho;");
        html4References.addReference('\u03C2', "&sigmaf;");
        html4References.addReference('\u03C3', "&sigma;");
        html4References.addReference('\u03C4', "&tau;");
        html4References.addReference('\u03C5', "&upsilon;");
        html4References.addReference('\u03C6', "&phi;");
        html4References.addReference('\u03C7', "&chi;");
        html4References.addReference('\u03C8', "&psi;");
        html4References.addReference('\u03C9', "&omega;");
        html4References.addReference('\u03D1', "&thetasym;");
        html4References.addReference('\u03D2', "&upsih;");
        html4References.addReference('\u03D6', "&piv;");
        /* - General punctuation */
        html4References.addReference('\u2022', "&bull;");
        html4References.addReference('\u2026', "&hellip;");
        html4References.addReference('\u2032', "&prime;");
        html4References.addReference('\u2033', "&Prime;");
        html4References.addReference('\u203E', "&oline;");
        html4References.addReference('\u2044', "&frasl;");
        /* - Letter-like symbols */
        html4References.addReference('\u2118', "&weierp;");
        html4References.addReference('\u2111', "&image;");
        html4References.addReference('\u211C', "&real;");
        html4References.addReference('\u2122', "&trade;");
        html4References.addReference('\u2135', "&alefsym;");
        /* - Arrows */
        html4References.addReference('\u2190', "&larr;");
        html4References.addReference('\u2191', "&uarr;");
        html4References.addReference('\u2192', "&rarr;");
        html4References.addReference('\u2193', "&darr;");
        html4References.addReference('\u2194', "&harr;");
        html4References.addReference('\u21B5', "&crarr;");
        html4References.addReference('\u21D0', "&lArr;");
        html4References.addReference('\u21D1', "&uArr;");
        html4References.addReference('\u21D2', "&rArr;");
        html4References.addReference('\u21D3', "&dArr;");
        html4References.addReference('\u21D4', "&hArr;");
        /* - Mathematical operators */
        html4References.addReference('\u2200', "&forall;");
        html4References.addReference('\u2202', "&part;");
        html4References.addReference('\u2203', "&exist;");
        html4References.addReference('\u2205', "&empty;");
        html4References.addReference('\u2207', "&nabla;");
        html4References.addReference('\u2208', "&isin;");
        html4References.addReference('\u2209', "&notin;");
        html4References.addReference('\u220B', "&ni;");
        html4References.addReference('\u220F', "&prod;");
        html4References.addReference('\u2211', "&sum;");
        html4References.addReference('\u2212', "&minus;");
        html4References.addReference('\u2217', "&lowast;");
        html4References.addReference('\u221A', "&radic;");
        html4References.addReference('\u221D', "&prop;");
        html4References.addReference('\u221E', "&infin;");
        html4References.addReference('\u2220', "&ang;");
        html4References.addReference('\u2227', "&and;");
        html4References.addReference('\u2228', "&or;");
        html4References.addReference('\u2229', "&cap;");
        html4References.addReference('\u222A', "&cup;");
        html4References.addReference('\u222B', "&int;");
        html4References.addReference('\u2234', "&there4;");
        html4References.addReference('\u223C', "&sim;");
        html4References.addReference('\u2245', "&cong;");
        html4References.addReference('\u2248', "&asymp;");
        html4References.addReference('\u2260', "&ne;");
        html4References.addReference('\u2261', "&equiv;");
        html4References.addReference('\u2264', "&le;");
        html4References.addReference('\u2265', "&ge;");
        html4References.addReference('\u2282', "&sub;");
        html4References.addReference('\u2283', "&sup;");
        html4References.addReference('\u2284', "&nsub;");
        html4References.addReference('\u2286', "&sube;");
        html4References.addReference('\u2287', "&supe;");
        html4References.addReference('\u2295', "&oplus;");
        html4References.addReference('\u2297', "&otimes;");
        html4References.addReference('\u22A5', "&perp;");
        html4References.addReference('\u22C5', "&sdot;");
        /* - Miscellaneous technical */
        html4References.addReference('\u2308', "&lceil;");
        html4References.addReference('\u2309', "&rceil;");
        html4References.addReference('\u230A', "&lfloor;");
        html4References.addReference('\u230B', "&rfloor;");
        html4References.addReference('\u2329', "&lang;");
        html4References.addReference('\u232A', "&rang;");
        /* - Geometric shapes */
        html4References.addReference('\u25CA', "&loz;");
        html4References.addReference('\u2660', "&spades;");
        html4References.addReference('\u2663', "&clubs;");
        html4References.addReference('\u2665', "&hearts;");
        html4References.addReference('\u2666', "&diams;");
        /* HTML NCRs FOR INTERNATIONALIZATION CHARACTERS */
        /* - Latin Extended-A */
        html4References.addReference('\u0152', "&OElig;");
        html4References.addReference('\u0153', "&oelig;");
        html4References.addReference('\u0160', "&Scaron;");
        html4References.addReference('\u0161', "&scaron;");
        html4References.addReference('\u0178', "&Yuml;");
        /* - Spacing modifier letters */
        html4References.addReference('\u02C6', "&circ;");
        html4References.addReference('\u02DC', "&tilde;");
        /* - General punctuation */
        html4References.addReference('\u2002', "&ensp;");
        html4References.addReference('\u2003', "&emsp;");
        html4References.addReference('\u2009', "&thinsp;");
        html4References.addReference('\u200C', "&zwnj;");
        html4References.addReference('\u200D', "&zwj;");
        html4References.addReference('\u200E', "&lrm;");
        html4References.addReference('\u200F', "&rlm;");
        html4References.addReference('\u2013', "&ndash;");
        html4References.addReference('\u2014', "&mdash;");
        html4References.addReference('\u2018', "&lsquo;");
        html4References.addReference('\u2019', "&rsquo;");
        html4References.addReference('\u201A', "&sbquo;");
        html4References.addReference('\u201C', "&ldquo;");
        html4References.addReference('\u201D', "&rdquo;");
        html4References.addReference('\u201E', "&bdquo;");
        html4References.addReference('\u2020', "&dagger;");
        html4References.addReference('\u2021', "&Dagger;");
        html4References.addReference('\u2030', "&permil;");
        html4References.addReference('\u2039', "&lsaquo;");
        html4References.addReference('\u203A', "&rsaquo;");
        html4References.addReference('\u20AC', "&euro;");


        /*
         * Initialization of escape levels.
         * Defined levels :
         *
         *    - Level 0 : Only markup-significant characters except the apostrophe (')
         *    - Level 1 : Only markup-significant characters (including the apostrophe)
         *    - Level 2 : Markup-significant characters plus all non-ASCII
         *    - Level 3 : All non-alphanumeric characters
         *    - Level 4 : All characters
         */
        final byte[] escapeLevels = new byte[0x7f + 2];
        Arrays.fill(escapeLevels, (byte) 3);
        for (char c = 'A'; c <= 'Z'; c++) {
            escapeLevels[c] = 4;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            escapeLevels[c] = 4;
        }
        for (char c = '0'; c <= '9'; c++) {
            escapeLevels[c] = 4;
        }
        escapeLevels['\''] = 1;
        escapeLevels['"'] = 0;
        escapeLevels['<'] = 0;
        escapeLevels['>'] = 0;
        escapeLevels['&'] = 0;
        escapeLevels[0x7f + 1] = 2;


        return new HtmlEscapeSymbols(html4References, escapeLevels);

    }

}

final class HtmlEscapeSymbols {


    /*
     * GLOSSARY
     * ------------------------
     *
     *   NCR
     *      Named Character Reference or Character Entity Reference: textual
     *      representation of an Unicode codepoint: &aacute;
     *
     *   DCR
     *      Decimal Character Reference: base-10 numerical representation of an Unicode codepoint: &#225;
     *
     *   HCR
     *      Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint: &#xE1;
     *
     *   Unicode Codepoint
     *      Each of the int values conforming the Unicode code space.
     *      Normally corresponding to a Java char primitive value (codepoint <= \uFFFF),
     *      but might be two chars for codepoints \u10000 to \u10FFFF if the first char is a high
     *      surrogate (\uD800 to \uDBFF) and the second is a low surrogate (\uDC00 to \uDFFF).
     *      See: http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html
     *
     */


    /*
     * Length of the array used for holding the 'base' NCRS indexed by the codepoints themselves. This size
     * (0x2fff - 12287) is considered enough to hold most of the NCRS that should be needed (HTML4 has 252
     * NCRs with a maximum codepoint of 0x2666 - HTML5 has 2125 NCRs with a maximum codepoint of 120171, but
     * only 138 scarcely used NCRs live above codepoint 0x2fff so an overflow map should be enough for
     * those 138 cases).
     */
    static final int NCRS_BY_CODEPOINT_LEN = 0x2fff;
    /*
     * Maximum char value inside the ASCII plane
     */
    static final char MAX_ASCII_CHAR = 0x7f;
    /*
     * This constant will be used at the NCRS_BY_CODEPOINT array to specify there is no NCR associated with a
     * codepoint.
     */
    static final short NO_NCR = (short) 0;
    /*
     * Constants holding the definition of all the HtmlEscapeSymbols for HTML4 and HTML5, to be used in escape and
     * unescape operations.
     */
    static final HtmlEscapeSymbols HTML4_SYMBOLS;
    static final HtmlEscapeSymbols HTML5_SYMBOLS;

    static {

        HTML4_SYMBOLS = Html4EscapeSymbolsInitializer.initializeHtml4();
        HTML5_SYMBOLS = Html5EscapeSymbolsInitializer.initializeHtml5();

    }

    /*
     * This array will contain the NCRs for the first NCRS_BY_CODEPOINT_LEN (0x2fff) codepoints, indexed by
     * the codepoints themselves so that they (even in the form of mere char's) can be used for array random access.
     * - Values are short in order to index values at the SORTED_NCRS array. This avoids the need for this
     *   array to hold String pointers, which would be 4 bytes in size each (compared to shorts, which are 2 bytes).
     * - Chars themselves or int codepoints can (will, in fact) be used as indexes.
     * - Given values are short, the maximum amount of total references this class can handle is 0x7fff = 32767
     *   (which is safe, because HTML5 has 2125).
     * - All XML and HTML4 NCRs will fit in this array. In the case of HTML5 NCRs, only 138 of the 2125 will
     *   not fit here (NCRs assigned to codepoints > 0x2fff), and an overflow map will be provided for them.
     * - Approximate size will be 16 (header) + 12287 * 2 = 24590 bytes.
     */
    final short[] NCRS_BY_CODEPOINT = new short[NCRS_BY_CODEPOINT_LEN];
    /*
     * This map will work as an overflow of the NCRS_BY_CODEPOINT array, so that the codepoint-to-NCR relation is
     * stored here (with hash-based access) for codepoints >= NCRS_BY_CODEPOINT_LEN (0x2fff).
     * - The use of a Map here still allows for reasonabily fast access for those rare cases in which codepoints above
     *   0x2fff are used.
     * - In the real world, this map will contain the 138 values needed by HTML5 for codepoints >= 0x2fff.
     * - Approximate max size will be (being a complex object like a Map, it's a rough approximation):
     *   16 (header) + 138 * (16 (entry header) + 16*2 (key, value headers) + 4 (key) + 2 (value)) = 7468 bytes
     */
    final Map<Integer, Short> NCRS_BY_CODEPOINT_OVERFLOW;// No need to instantiate it until we know it's needed
    /*
     * This array will hold the 'escape level' assigned to each ASCII character (codepoint), 0x0 to 0x7f and also
     * a level for the rest of non-ASCII characters.
     * - These levels are used to configure how (and if) escape operations should ignore ASCII or non-ASCII
     *   characters, or escape them somehow if required.
     * - Each HtmlEscapeSymbols structure will define a different set of levels for ASCII chars, according to their needs.
     * - Position 0x7f + 1 represents all the non-ASCII characters. The specified value will determine whether
     *   all non-ASCII characters have to be escaped or not.
     */
    final byte[] ESCAPE_LEVELS = new byte[MAX_ASCII_CHAR + 2];
    /*
     * This array will contain all the NCRs, alphabetically ordered.
     * - Positions in this array will correspond to positions in the SORTED_CODEPOINTS array, so that one array
     *   (this one) holds the NCRs while the other one holds the codepoint(s) such NCRs refer to.
     * - Gives the opportunity to store all NCRs in alphabetical order and therefore be able to perform
     *   binary search operations in order to quickly find NCRs (and translate to codepoints) when unescaping.
     * - Note this array will contain:
     *     * All NCRs referenced from NCRS_BY_CODEPOINT
     *     * NCRs whose codepoint is >= 0x2fff and therefore live in NCRS_BY_CODEPOINT_OVERFLOW
     *     * NCRs which are not referenced in any of the above because they are a shortcut for (and completely
     *       equivalent to) a sequence of two codepoints. These NCRs will only be unescaped, but never escaped.
     * - Max size in real world, when populated for HTML5: 2125 NCRs * 4 bytes/objref -> 8500 bytes, plus the texts.
     */
    final char[][] SORTED_NCRS;
    /*
     * This array contains all the codepoints corresponding to the NCRs stored in SORTED_NCRS. This array is ordered
     * so that each index in SORTED_NCRS can also be used to retrieve the corresponding CODEPOINT when used on this array.
     * - Values in this array can be positive (= single codepoint) or negative (= double codepoint, will need further
     *   resolution by means of the DOUBLE_CODEPOINTS array)
     * - Max size in real world, when populated for HTML5: 2125 NCRs * 4 bytes/objref -> 8500 bytes.
     */
    final int[] SORTED_CODEPOINTS;
    /*
     * This array stores the sequences of two codepoints that are escaped as a single NCR. The indexes of this array are
     * referenced as negative numbers at the SORTED_CODEPOINTS array, and the values are int[2], containing the
     * sequence of codepoints. HTML4 has no NCRs like this, HTML5 has 93.
     * - Note this array is only used in UNESCAPE operations. Double-codepoint NCR escape is not performed because
     *   the resulting characters are exactly equivalent to the escape of the two codepoints separately.
     * - Max size in real world, when populated for HTML5 (rough approximate): 93 * (4 (ref) + 16 + 2 * 4) = 2604 bytes
     */
    final int[][] DOUBLE_CODEPOINTS;


    /*
     * Create a new HtmlEscapeSymbols structure. This will initialize all the structures needed to cover the
     * specified references and escape levels, including sorted arrays, overflow maps, etc.
     */
    HtmlEscapeSymbols(final HtmlEscapeSymbols.References references, final byte[] escapeLevels) {

        super();

        // Initialize ASCII escape levels: just copy the array
        System.arraycopy(escapeLevels, 0, ESCAPE_LEVELS, 0, (0x7f + 2));


        // Initialize some auxiliary structures
        final List<char[]> ncrs = new ArrayList<char[]>(references.references.size() + 5);
        final List<Integer> codepoints = new ArrayList<Integer>(references.references.size() + 5);
        final List<int[]> doubleCodepoints = new ArrayList<int[]>(100);
        final Map<Integer, Short> ncrsByCodepointOverflow = new HashMap<Integer, Short>(20);

        // For each reference, initialize its corresponding codepoint -> ncr and ncr -> codepoint structures
        for (final HtmlEscapeSymbols.Reference reference : references.references) {

            final char[] referenceNcr = reference.ncr;
            final int[] referenceCodepoints = reference.codepoints;

            ncrs.add(referenceNcr);

            if (referenceCodepoints.length == 1) {
                // Only one codepoint (might be > 1 chars, though), this is the normal case

                final int referenceCodepoint = referenceCodepoints[0];
                codepoints.add(Integer.valueOf(referenceCodepoint));

            } else if (referenceCodepoints.length == 2) {
                // Two codepoints, therefore this NCR will translate when unescaping into a two-codepoint
                // (probably two-char, too) sequence. We will use a negative codepoint value to signal this.

                doubleCodepoints.add(referenceCodepoints);
                // Will need to subtract one from its index when unescaping (codepoint = -1 -> position 0)
                codepoints.add(Integer.valueOf((-1) * doubleCodepoints.size()));

            } else {

                throw new RuntimeException(
                        "Unsupported codepoints #: " + referenceCodepoints.length + " for " + new String(referenceNcr));

            }

        }

        // We hadn't touched this array before. First thing to do is initialize it, as it will have a huge
        // amount of "empty" (i.e. non-assigned) values.
        Arrays.fill(NCRS_BY_CODEPOINT, NO_NCR);


        // We can initialize now these arrays that will hold the NCR-to-codepoint correspondence, but we cannot copy
        // them directly from our auxiliary structures because we need to order the NCRs alphabetically first.

        SORTED_NCRS = new char[ncrs.size()][];
        SORTED_CODEPOINTS = new int[codepoints.size()];

        final List<char[]> ncrsOrdered = new ArrayList<char[]>(ncrs);
        Collections.sort(ncrsOrdered, new Comparator<char[]>() {
            public int compare(final char[] o1, final char[] o2) {
                return HtmlEscapeSymbols.compare(o1, o2, 0, o2.length);
            }
        });

        for (short i = 0; i < SORTED_NCRS.length; i++) {

            final char[] ncr = ncrsOrdered.get(i);
            SORTED_NCRS[i] = ncr;

            for (short j = 0; j < SORTED_NCRS.length; j++) {

                if (Arrays.equals(ncr, ncrs.get(j))) {

                    final int cp = codepoints.get(j);
                    SORTED_CODEPOINTS[i] = cp;

                    if (cp > 0) {
                        // Not negative (i.e. not double-codepoint)
                        if (cp < NCRS_BY_CODEPOINT_LEN) {
                            // Not overflown
                            if (NCRS_BY_CODEPOINT[cp] == NO_NCR) {
                                // Only the first NCR for each codepoint will be used for escaping.
                                NCRS_BY_CODEPOINT[cp] = i;
                            } else {
                                final int positionOfCurrent = positionInList(ncrs, SORTED_NCRS[NCRS_BY_CODEPOINT[cp]]);
                                final int positionOfNew = positionInList(ncrs, ncr);
                                if (positionOfNew < positionOfCurrent) {
                                    // The order in which NCRs were originally specified in the references argument
                                    // marks which NCR should be used for escaping (the first one), if several NCRs
                                    // have the same codepoint.
                                    NCRS_BY_CODEPOINT[cp] = i;
                                }
                            }
                        } else {
                            // Codepoint should be overflown
                            ncrsByCodepointOverflow.put(Integer.valueOf(cp), Short.valueOf(i));
                        }
                    }

                    break;

                }

            }

        }


        // Only create the overflow map if it is really needed.
        if (ncrsByCodepointOverflow.size() > 0) {
            NCRS_BY_CODEPOINT_OVERFLOW = ncrsByCodepointOverflow;
        } else {
            NCRS_BY_CODEPOINT_OVERFLOW = null;
        }


        // Finally, the double-codepoints structure can be initialized, if really needed.
        if (doubleCodepoints.size() > 0) {
            DOUBLE_CODEPOINTS = new int[doubleCodepoints.size()][];
            for (int i = 0; i < DOUBLE_CODEPOINTS.length; i++) {
                DOUBLE_CODEPOINTS[i] = doubleCodepoints.get(i);
            }
        } else {
            DOUBLE_CODEPOINTS = null;
        }

    }


    /*
     * Utility method, used for determining which of the different NCRs for the same
     * codepoint (when there are many) was specified first, because that is the one
     * we should be using for escaping.
     * (Note all of the NCRs will be available for unescaping, obviously)
     */
    private static int positionInList(final List<char[]> list, final char[] element) {
        int i = 0;
        for (final char[] e : list) {
            if (Arrays.equals(e, element)) {
                return i;
            }
            i++;
        }
        return -1;
    }




    /*
     * These two methods (two versions: for String and for char[]) compare each of the candidate
     * text fragments with an NCR coming from the SORTED_NCRs array, during binary search operations.
     *
     * Note these methods not only perform a normal comparison (returning -1, 0 or 1), but will also
     * return a negative number < -10 when a partial match is possible, this is, when the specified text
     * fragment contains a complete NCR at its first chars but contains more chars afterwards. This is
     * useful for matching HTML5 NCRs which do not end in ; (like '&aacute'), which will come in bigger fragments
     * because the unescape method will have no way of differentiating the chars after the NCR from chars that
     * could be in fact part of the NCR. Also note that, in the case of a partial match, (-1) * (returnValue + 10)
     * will specify the number of matched chars.
     *
     * Note we will willingly alter order so that ';' goes always first (even before no-char). This will allow
     * proper functioning of the partial-matching mechanism for NCRs that can appear both with and without
     * a ';' suffix.
     */

    private static int compare(final char[] ncr, final String text, final int start, final int end) {
        final int textLen = end - start;
        final int maxCommon = Math.min(ncr.length, textLen);
        int i;
        // char 0 is discarded, will be & in both cases
        for (i = 1; i < maxCommon; i++) {
            final char tc = text.charAt(start + i);
            if (ncr[i] < tc) {
                if (tc == ';') {
                    return 1;
                }
                return -1;
            } else if (ncr[i] > tc) {
                if (ncr[i] == ';') {
                    return -1;
                }
                return 1;
            }
        }
        if (ncr.length > i) {
            if (ncr[i] == ';') {
                return -1;
            }
            return 1;
        }
        if (textLen > i) {
            if (text.charAt(start + i) == ';') {
                return 1;
            }
            // We have a partial match. Can be an NCR not finishing in a semicolon
            return -((textLen - i) + 10);
        }
        return 0;
    }

    private static int compare(final char[] ncr, final char[] text, final int start, final int end) {
        final int textLen = end - start;
        final int maxCommon = Math.min(ncr.length, textLen);
        int i;
        // char 0 is discarded, will be & in both cases
        for (i = 1; i < maxCommon; i++) {
            final char tc = text[start + i];
            if (ncr[i] < tc) {
                if (tc == ';') {
                    return 1;
                }
                return -1;
            } else if (ncr[i] > tc) {
                if (ncr[i] == ';') {
                    return -1;
                }
                return 1;
            }
        }
        if (ncr.length > i) {
            if (ncr[i] == ';') {
                return -1;
            }
            return 1;
        }
        if (textLen > i) {
            if (text[start + i] == ';') {
                return 1;
            }
            // We have a partial match. Can be an NCR not finishing in a semicolon
            return -((textLen - i) + 10);
        }
        return 0;
    }



    /*
     * These two methods (two versions: for String and for char[]) are used during unescape at the
     * {@link HtmlEscapeUtil} class in order to quickly find the NCR corresponding to a preselected fragment
     * of text (if there is such NCR).
     *
     * Note this operation supports partial matching (based on the above 'compare(...)' methods). That way,
     * if an exact match is not found but a partial match exists, the partial match will be returned.
     */

    static int binarySearch(final char[][] values,
                            final String text, final int start, final int end) {

        int low = 0;
        int high = values.length - 1;

        int partialIndex = Integer.MIN_VALUE;
        int partialValue = Integer.MIN_VALUE;

        while (low <= high) {

            final int mid = (low + high) >>> 1;
            final char[] midVal = values[mid];

            final int cmp = compare(midVal, text, start, end);

            if (cmp == -1) {
                low = mid + 1;
            } else if (cmp == 1) {
                high = mid - 1;
            } else if (cmp < -10) {
                // Partial match
                low = mid + 1;
                if (partialIndex == Integer.MIN_VALUE || partialValue < cmp) {
                    partialIndex = mid;
                    partialValue = cmp; // partial will always be negative, and -10. We look for the smallest partial
                }
            } else {
                // Found!!
                return mid;
            }

        }

        if (partialIndex != Integer.MIN_VALUE) {
            // We have a partial result. We return the closest result index as negative + (-10)
            return (-1) * (partialIndex + 10);
        }

        return Integer.MIN_VALUE; // Not found!

    }

    static int binarySearch(final char[][] values,
                            final char[] text, final int start, final int end) {

        int low = 0;
        int high = values.length - 1;

        int partialIndex = Integer.MIN_VALUE;
        int partialValue = Integer.MIN_VALUE;

        while (low <= high) {

            final int mid = (low + high) >>> 1;
            final char[] midVal = values[mid];

            final int cmp = compare(midVal, text, start, end);

            if (cmp == -1) {
                low = mid + 1;
            } else if (cmp == 1) {
                high = mid - 1;
            } else if (cmp < -10) {
                // Partial match
                low = mid + 1;
                if (partialIndex == Integer.MIN_VALUE || partialValue < cmp) {
                    partialIndex = mid;
                    partialValue = cmp; // partial will always be negative, and -10. We look for the smallest partial
                }
            } else {
                // Found!!
                return mid;
            }

        }

        if (partialIndex != Integer.MIN_VALUE) {
            // We have a partial result. We return the closest result index as negative + (-10)
            return (-1) * (partialIndex + 10);
        }

        return Integer.MIN_VALUE; // Not found!

    }






    /*
     * Inner utility classes that model the named character references to be included in an initialized
     * instance of the HtmlEscapeSymbols class.
     */


    static final class References {

        private final List<HtmlEscapeSymbols.Reference> references = new ArrayList<HtmlEscapeSymbols.Reference>(200);

        References() {
            super();
        }

        void addReference(final int codepoint, final String ncr) {
            this.references.add(new HtmlEscapeSymbols.Reference(ncr, new int[]{codepoint}));
        }

        void addReference(final int codepoint0, final int codepoint1, final String ncr) {
            this.references.add(new HtmlEscapeSymbols.Reference(ncr, new int[]{codepoint0, codepoint1}));
        }

    }


    private static final class Reference {

        private final char[] ncr;
        private final int[] codepoints;

        private Reference(final String ncr, final int[] codepoints) {
            super();
            this.ncr = ncr.toCharArray();
            this.codepoints = codepoints;
        }

    }


}

final class HtmlEscapeUtil {



    /*
     * GLOSSARY
     * ------------------------
     *
     *   NCR
     *      Named Character Reference or Character Entity Reference: textual
     *      representation of an Unicode codepoint: &aacute;
     *
     *   DCR
     *      Decimal Character Reference: base-10 numerical representation of an Unicode codepoint: &#225;
     *
     *   HCR
     *      Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint: &#xE1;
     *
     *   Unicode Codepoint
     *      Each of the int values conforming the Unicode code space.
     *      Normally corresponding to a Java char primitive value (codepoint <= \uFFFF),
     *      but might be two chars for codepoints \u10000 to \u10FFFF if the first char is a high
     *      surrogate (\uD800 to \uDBFF) and the second is a low surrogate (\uDC00 to \uDFFF).
     *      See: http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html
     *
     */


    /*
     * Prefixes and suffix defined for use in decimal/hexa escape and unescape.
     */
    private static final char REFERENCE_PREFIX = '&';
    private static final char REFERENCE_NUMERIC_PREFIX2 = '#';
    private static final char REFERENCE_HEXA_PREFIX3_UPPER = 'X';
    private static final char REFERENCE_HEXA_PREFIX3_LOWER = 'x';
    private static final char[] REFERENCE_DECIMAL_PREFIX = "&#".toCharArray();
    private static final char[] REFERENCE_HEXA_PREFIX = "&#x".toCharArray();
    private static final char REFERENCE_SUFFIX = ';';

    /*
     * Small utility char arrays for hexadecimal conversion
     */
    private static final char[] HEXA_CHARS_UPPER = "0123456789ABCDEF".toCharArray();
    private static final char[] HEXA_CHARS_LOWER = "0123456789abcdef".toCharArray();


    private HtmlEscapeUtil() {
        super();
    }


    /*
     * Perform an escape operation, based on String, according to the specified level and type.
     */
    static String escape(final String text, final HtmlEscapeType escapeType, final HtmlEscapeLevel escapeLevel) {

        if (text == null) {
            return null;
        }

        final int level = escapeLevel.getEscapeLevel();
        final boolean useHtml5 = escapeType.getUseHtml5();
        final boolean useNCRs = escapeType.getUseNCRs();
        final boolean useHexa = escapeType.getUseHexa();

        final HtmlEscapeSymbols symbols =
                (useHtml5 ? HtmlEscapeSymbols.HTML5_SYMBOLS : HtmlEscapeSymbols.HTML4_SYMBOLS);

        StringBuilder strBuilder = null;

        final int offset = 0;
        final int max = text.length();

        int readOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = text.charAt(i);


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (c <= HtmlEscapeSymbols.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[c]) {
                continue;
            }


            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (c > HtmlEscapeSymbols.MAX_ASCII_CHAR
                    && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR + 1]) {
                continue;
            }


            /*
             * Compute the codepoint. This will be used instead of the char for the rest of the process.
             */
            final int codepoint = Character.codePointAt(text, i);


            /*
             * At this point we know for sure we will need some kind of escape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */

            if (strBuilder == null) {
                strBuilder = new StringBuilder(max + 20);
            }

            if (i - readOffset > 0) {
                strBuilder.append(text, readOffset, i);
            }

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually escaping two char[] positions with a single codepoint.
                i++;
            }

            readOffset = i + 1;


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
             *
             * -----------------------------------------------------------------------------------------
             */

            if (useNCRs) {
                // We will try to use an NCR

                if (codepoint < HtmlEscapeSymbols.NCRS_BY_CODEPOINT_LEN) {
                    // codepoint < 0x2fff - all HTML4, most HTML5

                    final short ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint];
                    if (ncrIndex != HtmlEscapeSymbols.NO_NCR) {
                        // There is an NCR for this codepoint!
                        strBuilder.append(symbols.SORTED_NCRS[ncrIndex]);
                        continue;
                    } // else, just let it exit the block and let decimal/hexa escape do its job

                } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
                    // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

                    final Short ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(Integer.valueOf(codepoint));
                    if (ncrIndex != null) {
                        strBuilder.append(symbols.SORTED_NCRS[ncrIndex.shortValue()]);
                        continue;
                    } // else, just let it exit the block and let decimal/hexa escape do its job

                }

            }

            /*
             * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
             */

            if (useHexa) {
                strBuilder.append(REFERENCE_HEXA_PREFIX);
                strBuilder.append(Integer.toHexString(codepoint));
            } else {
                strBuilder.append(REFERENCE_DECIMAL_PREFIX);
                strBuilder.append(codepoint);
            }
            strBuilder.append(REFERENCE_SUFFIX);

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no escape was actually needed. Otherwise
         *                 append the remaining unescaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (strBuilder == null) {
            return text;
        }

        if (max - readOffset > 0) {
            strBuilder.append(text, readOffset, max);
        }

        return strBuilder.toString();

    }


    /*
     * Perform an escape operation, based on a Reader, according to the specified level and type and writing the
     * result to a Writer.
     *
     * Note this reader is going to be read char-by-char, so some kind of buffering might be appropriate if this
     * is an inconvenience for the specific Reader implementation.
     */
    static void escape(
            final Reader reader, final Writer writer, final HtmlEscapeType escapeType, final HtmlEscapeLevel escapeLevel)
            throws IOException {

        if (reader == null) {
            return;
        }

        final int level = escapeLevel.getEscapeLevel();
        final boolean useHtml5 = escapeType.getUseHtml5();
        final boolean useNCRs = escapeType.getUseNCRs();
        final boolean useHexa = escapeType.getUseHexa();

        final HtmlEscapeSymbols symbols =
                (useHtml5 ? HtmlEscapeSymbols.HTML5_SYMBOLS : HtmlEscapeSymbols.HTML4_SYMBOLS);

        int c1, c2; // c1: current char, c2: next char

        c2 = reader.read();

        while (c2 >= 0) {

            c1 = c2;
            c2 = reader.read();


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (c1 <= HtmlEscapeSymbols.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[c1]) {
                writer.write(c1);
                continue;
            }


            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (c1 > HtmlEscapeSymbols.MAX_ASCII_CHAR
                    && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR + 1]) {
                writer.write(c1);
                continue;
            }


            /*
             * Compute the codepoint. This will be used instead of the char for the rest of the process.
             */
            final int codepoint = codePointAt((char) c1, (char) c2);


            /*
             * We know we need to escape, so from here on we will only work with the codepoint -- we can advance
             * the chars.
             */

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually reading two char positions with a single codepoint.
                c1 = c2;
                c2 = reader.read();
            }


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
             *
             * -----------------------------------------------------------------------------------------
             */

            if (useNCRs) {
                // We will try to use an NCR

                if (codepoint < HtmlEscapeSymbols.NCRS_BY_CODEPOINT_LEN) {
                    // codepoint < 0x2fff - all HTML4, most HTML5

                    final short ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint];
                    if (ncrIndex != HtmlEscapeSymbols.NO_NCR) {
                        // There is an NCR for this codepoint!
                        writer.write(symbols.SORTED_NCRS[ncrIndex]);
                        continue;
                    } // else, just let it exit the block and let decimal/hexa escape do its job

                } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
                    // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

                    final Short ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(Integer.valueOf(codepoint));
                    if (ncrIndex != null) {
                        writer.write(symbols.SORTED_NCRS[ncrIndex.shortValue()]);
                        continue;
                    } // else, just let it exit the block and let decimal/hexa escape do its job

                }

            }

            /*
             * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
             */

            if (useHexa) {
                writer.write(REFERENCE_HEXA_PREFIX);
                writer.write(Integer.toHexString(codepoint));
            } else {
                writer.write(REFERENCE_DECIMAL_PREFIX);
                writer.write(String.valueOf(codepoint));
            }
            writer.write(REFERENCE_SUFFIX);

        }

    }


    /*
     * Perform an escape operation, based on char[], according to the specified level and type.
     */
    static void escape(final char[] text, final int offset, final int len, final Writer writer,
                       final HtmlEscapeType escapeType, final HtmlEscapeLevel escapeLevel)
            throws IOException {

        if (text == null || text.length == 0) {
            return;
        }

        final int level = escapeLevel.getEscapeLevel();
        final boolean useHtml5 = escapeType.getUseHtml5();
        final boolean useNCRs = escapeType.getUseNCRs();
        final boolean useHexa = escapeType.getUseHexa();

        final HtmlEscapeSymbols symbols =
                (useHtml5 ? HtmlEscapeSymbols.HTML5_SYMBOLS : HtmlEscapeSymbols.HTML4_SYMBOLS);

        final int max = (offset + len);

        int readOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = text[i];


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (c <= HtmlEscapeSymbols.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[c]) {
                continue;
            }


            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (c > HtmlEscapeSymbols.MAX_ASCII_CHAR && level < symbols.ESCAPE_LEVELS[HtmlEscapeSymbols.MAX_ASCII_CHAR + 1]) {
                continue;
            }


            /*
             * Compute the codepoint. This will be used instead of the char for the rest of the process.
             */
            final int codepoint = Character.codePointAt(text, i);


            /*
             * At this point we know for sure we will need some kind of escape, so we
             * can write all the contents pending up to this point.
             */

            if (i - readOffset > 0) {
                writer.write(text, readOffset, (i - readOffset));
            }

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually escaping two char[] positions with a single codepoint.
                i++;
            }

            readOffset = i + 1;


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Perform the real escape, attending the different combinations of NCR, DCR and HCR needs.
             *
             * -----------------------------------------------------------------------------------------
             */

            if (useNCRs) {
                // We will try to use an NCR

                if (codepoint < HtmlEscapeSymbols.NCRS_BY_CODEPOINT_LEN) {
                    // codepoint < 0x2fff - all HTML4, most HTML5

                    final short ncrIndex = symbols.NCRS_BY_CODEPOINT[codepoint];
                    if (ncrIndex != HtmlEscapeSymbols.NO_NCR) {
                        // There is an NCR for this codepoint!
                        writer.write(symbols.SORTED_NCRS[ncrIndex]);
                        continue;
                    } // else, just let it exit the block and let decimal/hexa escape do its job

                } else if (symbols.NCRS_BY_CODEPOINT_OVERFLOW != null) {
                    // codepoint >= 0x2fff. NCR, if exists, will live at the overflow map (if there is one).

                    final Short ncrIndex = symbols.NCRS_BY_CODEPOINT_OVERFLOW.get(Integer.valueOf(codepoint));
                    if (ncrIndex != null) {
                        writer.write(symbols.SORTED_NCRS[ncrIndex.shortValue()]);
                        continue;
                    } // else, just let it exit the block and let decimal/hexa escape do its job

                }

            }

            /*
             * No NCR-escape was possible (or allowed), so we need decimal/hexa escape.
             */

            if (useHexa) {
                writer.write(REFERENCE_HEXA_PREFIX);
                writer.write(Integer.toHexString(codepoint));
            } else {
                writer.write(REFERENCE_DECIMAL_PREFIX);
                writer.write(String.valueOf(codepoint));
            }
            writer.write(REFERENCE_SUFFIX);

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: append the remaining unescaped text to the writer and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (max - readOffset > 0) {
            writer.write(text, readOffset, (max - readOffset));
        }

    }


    /*
     * This translation is needed during unescape to support ill-formed escape codes for Windows 1252 codes
     * instead of the correct unicode ones (for example, &#x80; for the euro symbol instead of &#x20aC;). This is
     * something browsers do support, and included in the HTML5 spec for consuming character references.
     * See http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     */
    static int translateIllFormedCodepoint(final int codepoint) {
        switch (codepoint) {
            case 0x00:
                return 0xFFFD;
            case 0x80:
                return 0x20AC;
            case 0x82:
                return 0x201A;
            case 0x83:
                return 0x0192;
            case 0x84:
                return 0x201E;
            case 0x85:
                return 0x2026;
            case 0x86:
                return 0x2020;
            case 0x87:
                return 0x2021;
            case 0x88:
                return 0x02C6;
            case 0x89:
                return 0x2030;
            case 0x8A:
                return 0x0160;
            case 0x8B:
                return 0x2039;
            case 0x8C:
                return 0x0152;
            case 0x8E:
                return 0x017D;
            case 0x91:
                return 0x2018;
            case 0x92:
                return 0x2019;
            case 0x93:
                return 0x201C;
            case 0x94:
                return 0x201D;
            case 0x95:
                return 0x2022;
            case 0x96:
                return 0x2013;
            case 0x97:
                return 0x2014;
            case 0x98:
                return 0x02DC;
            case 0x99:
                return 0x2122;
            case 0x9A:
                return 0x0161;
            case 0x9B:
                return 0x203A;
            case 0x9C:
                return 0x0153;
            case 0x9E:
                return 0x017E;
            case 0x9F:
                return 0x0178;
            default:
                break;
        }
        if (codepoint >= 0xD800 && codepoint <= 0xDFFF) {
            return 0xFFFD;
        } else if (codepoint > 0x10FFFF) {
            return 0xFFFD;
        } else {
            return codepoint;
        }
    }


    /*
     * This methods (the two versions) are used instead of Integer.parseInt(str,radix) in order to avoid the need
     * to create substrings of the text being unescaped to feed such method.
     * -  No need to check all chars are within the radix limits - reference parsing code will already have done so.
     */

    static int parseIntFromReference(final String text, final int start, final int end, final int radix) {
        int result = 0;
        for (int i = start; i < end; i++) {
            final char c = text.charAt(i);
            int n = -1;
            for (int j = 0; j < HEXA_CHARS_UPPER.length; j++) {
                if (c == HEXA_CHARS_UPPER[j] || c == HEXA_CHARS_LOWER[j]) {
                    n = j;
                    break;
                }
            }
            result *= radix;
            if (result < 0) {
                return 0xFFFD;
            }
            result += n;
            if (result < 0) {
                return 0xFFFD;
            }
        }
        return result;
    }

    static int parseIntFromReference(final char[] text, final int start, final int end, final int radix) {
        int result = 0;
        for (int i = start; i < end; i++) {
            final char c = text[i];
            int n = -1;
            for (int j = 0; j < HEXA_CHARS_UPPER.length; j++) {
                if (c == HEXA_CHARS_UPPER[j] || c == HEXA_CHARS_LOWER[j]) {
                    n = j;
                    break;
                }
            }
            result *= radix;
            if (result < 0) {
                return 0xFFFD;
            }
            result += n;
            if (result < 0) {
                return 0xFFFD;
            }
        }
        return result;
    }


    /*
     * Perform an unescape operation based on String. Unescape operations are always based on the HTML5 symbol set.
     * Unescape operations will be performed in the most similar way possible to the process a browser follows for
     * showing HTML5 escaped code. See: http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     */
    static String unescape(final String text) {

        if (text == null) {
            return null;
        }

        // Unescape will always cover the full HTML5 spectrum.
        final HtmlEscapeSymbols symbols = HtmlEscapeSymbols.HTML5_SYMBOLS;
        StringBuilder strBuilder = null;

        final int offset = 0;
        final int max = text.length();

        int readOffset = offset;
        int referenceOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = text.charAt(i);

            /*
             * Check the need for an unescape operation at this point
             */

            if (c != REFERENCE_PREFIX || (i + 1) >= max) {
                continue;
            }

            int codepoint = 0;

            if (c == REFERENCE_PREFIX) {

                final char c1 = text.charAt(i + 1);

                if (c1 == '\u0020' || // SPACE
                        c1 == '\n' ||     // LF
                        c1 == '\u0009' || // TAB
                        c1 == '\u000C' || // FF
                        c1 == '\u003C' || // LES-THAN SIGN
                        c1 == '\u0026') { // AMPERSAND
                    // Not a character references. No characters are consumed, and nothing is returned.
                    continue;

                } else if (c1 == REFERENCE_NUMERIC_PREFIX2) {

                    if (i + 2 >= max) {
                        // No reference possible
                        continue;
                    }

                    final char c2 = text.charAt(i + 2);

                    if ((c2 == REFERENCE_HEXA_PREFIX3_LOWER || c2 == REFERENCE_HEXA_PREFIX3_UPPER) && (i + 3) < max) {
                        // This is a hexadecimal reference

                        int f = i + 3;
                        while (f < max) {
                            final char cf = text.charAt(f);
                            if (!((cf >= '0' && cf <= '9') || (cf >= 'A' && cf <= 'F') || (cf >= 'a' && cf <= 'f'))) {
                                break;
                            }
                            f++;
                        }

                        if ((f - (i + 3)) <= 0) {
                            // We weren't able to consume any hexa chars
                            continue;
                        }

                        codepoint = parseIntFromReference(text, i + 3, f, 16);
                        referenceOffset = f - 1;

                        if ((f < max) && text.charAt(f) == REFERENCE_SUFFIX) {
                            referenceOffset++;
                        }

                        codepoint = translateIllFormedCodepoint(codepoint);

                        // Don't continue here, just let the unescape code below do its job

                    } else if (c2 >= '0' && c2 <= '9') {
                        // This is a decimal reference

                        int f = i + 2;
                        while (f < max) {
                            final char cf = text.charAt(f);
                            if (!(cf >= '0' && cf <= '9')) {
                                break;
                            }
                            f++;
                        }

                        if ((f - (i + 2)) <= 0) {
                            // We weren't able to consume any decimal chars
                            continue;
                        }

                        codepoint = parseIntFromReference(text, i + 2, f, 10);
                        referenceOffset = f - 1;

                        if ((f < max) && text.charAt(f) == REFERENCE_SUFFIX) {
                            referenceOffset++;
                        }

                        codepoint = translateIllFormedCodepoint(codepoint);

                        // Don't continue here, just let the unescape code below do its job

                    } else {
                        // This is not a valid reference, just discard
                        continue;
                    }


                } else {

                    // This is a named reference, must be comprised only of ALPHABETIC chars

                    int f = i + 1;
                    while (f < max) {
                        final char cf = text.charAt(f);
                        if (!((cf >= 'a' && cf <= 'z') || (cf >= 'A' && cf <= 'Z') || (cf >= '0' && cf <= '9'))) {
                            break;
                        }
                        f++;
                    }

                    if ((f - (i + 1)) <= 0) {
                        // We weren't able to consume any alphanumeric
                        continue;
                    }

                    if ((f < max) && text.charAt(f) == REFERENCE_SUFFIX) {
                        f++;
                    }

                    final int ncrPosition = HtmlEscapeSymbols.binarySearch(symbols.SORTED_NCRS, text, i, f);
                    if (ncrPosition >= 0) {
                        codepoint = symbols.SORTED_CODEPOINTS[ncrPosition];
                    } else if (ncrPosition == Integer.MIN_VALUE) {
                        // Not found! Just ignore our efforts to find a match.
                        continue;
                    } else if (ncrPosition < -10) {
                        // Found but partial!
                        final int partialIndex = (-1) * (ncrPosition + 10);
                        final char[] partialMatch = symbols.SORTED_NCRS[partialIndex];
                        codepoint = symbols.SORTED_CODEPOINTS[partialIndex];
                        f -= ((f - i) - partialMatch.length); // un-consume the chars remaining from the partial match
                    } else {
                        // Should never happen!
                        throw new RuntimeException("Invalid unescape codepoint after search: " + ncrPosition);
                    }

                    referenceOffset = f - 1;

                }

            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */

            if (strBuilder == null) {
                strBuilder = new StringBuilder(max + 5);
            }

            if (i - readOffset > 0) {
                strBuilder.append(text, readOffset, i);
            }

            i = referenceOffset;
            readOffset = i + 1;

            /*
             * --------------------------
             *
             * Perform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                strBuilder.append(Character.toChars(codepoint));
            } else if (codepoint < 0) {
                // This is a double-codepoint unescape operation
                final int[] codepoints = symbols.DOUBLE_CODEPOINTS[((-1) * codepoint) - 1];
                if (codepoints[0] > '\uFFFF') {
                    strBuilder.append(Character.toChars(codepoints[0]));
                } else {
                    strBuilder.append((char) codepoints[0]);
                }
                if (codepoints[1] > '\uFFFF') {
                    strBuilder.append(Character.toChars(codepoints[1]));
                } else {
                    strBuilder.append((char) codepoints[1]);
                }
            } else {
                strBuilder.append((char) codepoint);
            }

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no unescape was actually needed. Otherwise
         *                 append the remaining escaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (strBuilder == null) {
            return text;
        }

        if (max - readOffset > 0) {
            strBuilder.append(text, readOffset, max);
        }

        return strBuilder.toString();

    }


    /*
     * Perform an unescape operation based on a Reader, writing the results to a Writer. Unescape operations are
     * always based on the HTML5 symbol set. Unescape operations will be performed in the most similar way
     * possible to the process a browser follows for showing HTML5 escaped code.
     * See: http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     *
     * Note this reader is going to be read char-by-char, so some kind of buffering might be appropriate if this
     * is an inconvenience for the specific Reader implementation.
     */
    static void unescape(final Reader reader, final Writer writer) throws IOException {

        if (reader == null) {
            return;
        }

        // Unescape will always cover the full HTML5 spectrum.
        final HtmlEscapeSymbols symbols = HtmlEscapeSymbols.HTML5_SYMBOLS;

        char[] escapes = new char[10];
        int escapei = 0;

        int c1, c2, ce; // c1: current char, c2: next char, ce: current escaped char

        c2 = reader.read();

        while (c2 >= 0) {

            c1 = c2;
            c2 = reader.read();

            escapei = 0;

            /*
             * Check the need for an unescape operation at this point
             */

            if (c1 != REFERENCE_PREFIX || c2 < 0) {
                writer.write(c1);
                continue;
            }

            int codepoint = 0;

            if (c1 == REFERENCE_PREFIX) {

                if (c2 == '\u0020' || // SPACE
                        c2 == '\n' ||     // LF
                        c2 == '\u0009' || // TAB
                        c2 == '\u000C' || // FF
                        c2 == '\u003C' || // LES-THAN SIGN
                        c2 == '\u0026') { // AMPERSAND
                    // Not a character references. No characters are consumed, and nothing is returned.
                    writer.write(c1);
                    continue;

                } else if (c2 == REFERENCE_NUMERIC_PREFIX2) {

                    final int c3 = reader.read();

                    if (c3 < 0) {
                        // No reference possible
                        writer.write(c1);
                        writer.write(c2);
                        c1 = c2;
                        c2 = c3;
                        continue;
                    }

                    if ((c3 == REFERENCE_HEXA_PREFIX3_LOWER || c3 == REFERENCE_HEXA_PREFIX3_UPPER)) {
                        // This is a hexadecimal reference

                        ce = reader.read();
                        while (ce >= 0) {
                            if (!((ce >= '0' && ce <= '9') || (ce >= 'A' && ce <= 'F') || (ce >= 'a' && ce <= 'f'))) {
                                break;
                            }
                            if (escapei == escapes.length) {
                                // too many escape chars for our array: grow it!
                                final char[] newEscapes = new char[escapes.length + 4];
                                System.arraycopy(escapes, 0, newEscapes, 0, escapes.length);
                                escapes = newEscapes;
                            }
                            escapes[escapei] = (char) ce;
                            ce = reader.read();
                            escapei++;
                        }

                        if (escapei == 0) {
                            // We weren't able to consume any hexa chars
                            writer.write(c1);
                            writer.write(c2);
                            writer.write(c3);
                            c1 = c3;
                            c2 = ce;
                            continue;
                        }

                        c1 = escapes[escapei - 1];
                        c2 = ce;

                        codepoint = parseIntFromReference(escapes, 0, escapei, 16);

                        if (c2 == REFERENCE_SUFFIX) {
                            // If the reference ends in a ';', just consume it
                            c1 = c2;
                            c2 = reader.read();
                        }

                        codepoint = translateIllFormedCodepoint(codepoint);

                        escapei = 0;

                        // Don't continue here, just let the unescape code below do its job

                    } else if (c3 >= '0' && c3 <= '9') {
                        // This is a decimal reference

                        ce = c3;
                        while (ce >= 0) {
                            if (!(ce >= '0' && ce <= '9')) {
                                break;
                            }
                            if (escapei == escapes.length) {
                                // too many escape chars for our array: grow it!
                                final char[] newEscapes = new char[escapes.length + 4];
                                System.arraycopy(escapes, 0, newEscapes, 0, escapes.length);
                                escapes = newEscapes;
                            }
                            escapes[escapei] = (char) ce;
                            ce = reader.read();
                            escapei++;
                        }

                        if (escapei == 0) {
                            // We weren't able to consume any decimal chars
                            writer.write(c1);
                            writer.write(c2);
                            c1 = c2;
                            c2 = c3;
                            continue;
                        }

                        c1 = escapes[escapei - 1];
                        c2 = ce;

                        codepoint = parseIntFromReference(escapes, 0, escapei, 10);

                        if (c2 == REFERENCE_SUFFIX) {
                            // If the reference ends in a ';', just consume it
                            c1 = c2;
                            c2 = reader.read();
                        }

                        codepoint = translateIllFormedCodepoint(codepoint);

                        escapei = 0;

                        // Don't continue here, just let the unescape code below do its job

                    } else {
                        // This is not a valid reference, just discard
                        writer.write(c1);
                        writer.write(c2);
                        c1 = c2;
                        c2 = c3;
                        continue;
                    }


                } else {

                    // This is a named reference, must be comprised only of ALPHABETIC chars

                    ce = c2;
                    while (ce >= 0) {
                        if (!((ce >= '0' && ce <= '9') || (ce >= 'A' && ce <= 'Z') || (ce >= 'a' && ce <= 'z'))) {
                            break;
                        }
                        if (escapei == escapes.length) {
                            // too many escape chars for our array: grow it!
                            final char[] newEscapes = new char[escapes.length + 4];
                            System.arraycopy(escapes, 0, newEscapes, 0, escapes.length);
                            escapes = newEscapes;
                        }
                        escapes[escapei] = (char) ce;
                        ce = reader.read();
                        escapei++;
                    }

                    if (escapei == 0) {
                        // We weren't able to consume any decimal chars
                        writer.write(c1);
                        continue;
                    }

                    if (escapei + 2 >= escapes.length) {
                        // the entire escape sequence does not fit: grow it!
                        final char[] newEscapes = new char[escapes.length + 4];
                        System.arraycopy(escapes, 0, newEscapes, 0, escapes.length);
                        escapes = newEscapes;
                    }

                    System.arraycopy(escapes, 0, escapes, 1, escapei);
                    escapes[0] = (char) c1;
                    escapei++;

                    if (ce == REFERENCE_SUFFIX) {
                        // If the reference ends in a ';', just consume it
                        escapes[escapei++] = (char) ce;
                        ce = reader.read();
                    }

                    c1 = escapes[escapei - 1];
                    c2 = ce;

                    final int ncrPosition = HtmlEscapeSymbols.binarySearch(symbols.SORTED_NCRS, escapes, 0, escapei);
                    if (ncrPosition >= 0) {
                        codepoint = symbols.SORTED_CODEPOINTS[ncrPosition];
                        escapei = 0;
                    } else if (ncrPosition == Integer.MIN_VALUE) {
                        // Not found! Just ignore our efforts to find a match.
                        writer.write(escapes, 0, escapei);
                        continue;
                    } else if (ncrPosition < -10) {
                        // Found but partial!
                        final int partialIndex = (-1) * (ncrPosition + 10);
                        final char[] partialMatch = symbols.SORTED_NCRS[partialIndex];
                        codepoint = symbols.SORTED_CODEPOINTS[partialIndex];
                        System.arraycopy(escapes, partialMatch.length, escapes, 0, (escapei - partialMatch.length));
                        escapei -= partialMatch.length; // so that we know we have to output the rest of 'escapes'
                    } else {
                        // Should never happen!
                        throw new RuntimeException("Invalid unescape codepoint after search: " + ncrPosition);
                    }

                }

            }

            /*
             * --------------------------
             *
             * Perform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                writer.write(Character.toChars(codepoint));
            } else if (codepoint < 0) {
                // This is a double-codepoint unescape operation
                final int[] codepoints = symbols.DOUBLE_CODEPOINTS[((-1) * codepoint) - 1];
                if (codepoints[0] > '\uFFFF') {
                    writer.write(Character.toChars(codepoints[0]));
                } else {
                    writer.write((char) codepoints[0]);
                }
                if (codepoints[1] > '\uFFFF') {
                    writer.write(Character.toChars(codepoints[1]));
                } else {
                    writer.write((char) codepoints[1]);
                }
            } else {
                writer.write((char) codepoint);
            }

            /*
             * ----------------------------------------
             * Cleanup, in case we had a partial match
             * ----------------------------------------
             */

            if (escapei > 0) {
                writer.write(escapes, 0, escapei);
                escapei = 0;
            }


        }

    }


    /*
     * Perform an unescape operation based on char[]. Unescape operations are always based on the HTML5 symbol set.
     * Unescape operations will be performed in the most similar way possible to the process a browser follows for
     * showing HTML5 escaped code. See: http://www.w3.org/TR/html5/syntax.html#consume-a-character-reference
     */
    static void unescape(final char[] text, final int offset, final int len, final Writer writer)
            throws IOException {

        if (text == null) {
            return;
        }

        final HtmlEscapeSymbols symbols = HtmlEscapeSymbols.HTML5_SYMBOLS;

        final int max = (offset + len);

        int readOffset = offset;
        int referenceOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = text[i];

            /*
             * Check the need for an unescape operation at this point
             */

            if (c != REFERENCE_PREFIX || (i + 1) >= max) {
                continue;
            }

            int codepoint = 0;

            if (c == REFERENCE_PREFIX) {

                final char c1 = text[i + 1];

                if (c1 == '\u0020' || // SPACE
                        c1 == '\n' ||     // LF
                        c1 == '\u0009' || // TAB
                        c1 == '\u000C' || // FF
                        c1 == '\u003C' || // LES-THAN SIGN
                        c1 == '\u0026') { // AMPERSAND
                    // Not a character references. No characters are consumed, and nothing is returned.
                    continue;

                } else if (c1 == REFERENCE_NUMERIC_PREFIX2) {

                    if (i + 2 >= max) {
                        // No reference possible
                        continue;
                    }

                    final char c2 = text[i + 2];

                    if ((c2 == REFERENCE_HEXA_PREFIX3_LOWER || c2 == REFERENCE_HEXA_PREFIX3_UPPER) && (i + 3) < max) {
                        // This is a hexadecimal reference

                        int f = i + 3;
                        while (f < max) {
                            final char cf = text[f];
                            if (!((cf >= '0' && cf <= '9') || (cf >= 'A' && cf <= 'F') || (cf >= 'a' && cf <= 'f'))) {
                                break;
                            }
                            f++;
                        }

                        if ((f - (i + 3)) <= 0) {
                            // We weren't able to consume any hexa chars
                            continue;
                        }

                        codepoint = parseIntFromReference(text, i + 3, f, 16);
                        referenceOffset = f - 1;

                        if ((f < max) && text[f] == REFERENCE_SUFFIX) {
                            referenceOffset++;
                        }

                        codepoint = translateIllFormedCodepoint(codepoint);

                        // Don't continue here, just let the unescape code below do its job

                    } else if (c2 >= '0' && c2 <= '9') {
                        // This is a decimal reference

                        int f = i + 2;
                        while (f < max) {
                            final char cf = text[f];
                            if (!(cf >= '0' && cf <= '9')) {
                                break;
                            }
                            f++;
                        }

                        if ((f - (i + 2)) <= 0) {
                            // We weren't able to consume any decimal chars
                            continue;
                        }

                        codepoint = parseIntFromReference(text, i + 2, f, 10);
                        referenceOffset = f - 1;

                        if ((f < max) && text[f] == REFERENCE_SUFFIX) {
                            referenceOffset++;
                        }

                        codepoint = translateIllFormedCodepoint(codepoint);

                        // Don't continue here, just let the unescape code below do its job

                    } else {
                        // This is not a valid reference, just discard
                        continue;
                    }


                } else {

                    // This is a named reference, must be comprised only of ALPHABETIC chars

                    int f = i + 1;
                    while (f < max) {
                        final char cf = text[f];
                        if (!((cf >= 'a' && cf <= 'z') || (cf >= 'A' && cf <= 'Z') || (cf >= '0' && cf <= '9'))) {
                            break;
                        }
                        f++;
                    }

                    if ((f - (i + 1)) <= 0) {
                        // We weren't able to consume any alphanumeric
                        continue;
                    }

                    if ((f < max) && text[f] == REFERENCE_SUFFIX) {
                        f++;
                    }

                    final int ncrPosition = HtmlEscapeSymbols.binarySearch(symbols.SORTED_NCRS, text, i, f);
                    if (ncrPosition >= 0) {
                        codepoint = symbols.SORTED_CODEPOINTS[ncrPosition];
                    } else if (ncrPosition == Integer.MIN_VALUE) {
                        // Not found! Just ignore our efforts to find a match.
                        continue;
                    } else if (ncrPosition < -10) {
                        // Found but partial!
                        final int partialIndex = (-1) * (ncrPosition + 10);
                        final char[] partialMatch = symbols.SORTED_NCRS[partialIndex];
                        codepoint = symbols.SORTED_CODEPOINTS[partialIndex];
                        f -= ((f - i) - partialMatch.length); // un-consume the chars remaining from the partial match
                    } else {
                        // Should never happen!
                        throw new RuntimeException("Invalid unescape codepoint after search: " + ncrPosition);
                    }

                    referenceOffset = f - 1;

                }

            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * write all the contents pending up to this point.
             */

            if (i - readOffset > 0) {
                writer.write(text, readOffset, (i - readOffset));
            }

            i = referenceOffset;
            readOffset = i + 1;

            /*
             * --------------------------
             *
             * Perform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                writer.write(Character.toChars(codepoint));
            } else if (codepoint < 0) {
                // This is a double-codepoint unescape operation
                final int[] codepoints = symbols.DOUBLE_CODEPOINTS[((-1) * codepoint) - 1];
                if (codepoints[0] > '\uFFFF') {
                    writer.write(Character.toChars(codepoints[0]));
                } else {
                    writer.write((char) codepoints[0]);
                }
                if (codepoints[1] > '\uFFFF') {
                    writer.write(Character.toChars(codepoints[1]));
                } else {
                    writer.write((char) codepoints[1]);
                }
            } else {
                writer.write((char) codepoint);
            }

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: writer the remaining escaped text and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (max - readOffset > 0) {
            writer.write(text, readOffset, (max - readOffset));
        }

    }


    private static int codePointAt(final char c1, final char c2) {
        if (Character.isHighSurrogate(c1)) {
            if (c2 >= 0) {
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(c1, c2);
                }
            }
        }
        return c1;
    }


}

final class HtmlEscape {


    private HtmlEscape() {
        super();
    }

    /**
     * <p>
     * Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml5(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml4Xml(String)} because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     * preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml5Xml(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml4(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml5Xml(String)} because
     * it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     * such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(String, HtmlEscapeType, HtmlEscapeLevel)} with the following
     * preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml4Xml(final String text) {
        return escapeHtml(text, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     * This method will perform an escape operation according to the specified
     * {@link HtmlEscapeType} and {@link HtmlEscapeLevel}
     * argument values.
     * </p>
     * <p>
     * All other <tt>String</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text  the <tt>String</tt> to be escaped.
     * @param type  the type of escape operation to be performed, see {@link HtmlEscapeType}.
     * @param level the escape level to be applied, see {@link HtmlEscapeLevel}.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String escapeHtml(final String text, final HtmlEscapeType type, final HtmlEscapeLevel level) {

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        return HtmlEscapeUtil.escape(text, type, level);

    }

    /**
     * <p>
     * Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml5(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml4Xml(String, Writer)} because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     * preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml5Xml(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml4(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>String</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml5Xml(String, Writer)} because
     * it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     * such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(String, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     * preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml4Xml(final String text, final Writer writer)
            throws IOException {
        escapeHtml(text, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>String</tt> input, writing
     * results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * This method will perform an escape operation according to the specified
     * {@link HtmlEscapeType} and {@link HtmlEscapeLevel}
     * argument values.
     * </p>
     * <p>
     * All other <tt>String</tt>/<tt>Writer</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>String</tt> to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @param type   the type of escape operation to be performed, see {@link HtmlEscapeType}.
     * @param level  the escape level to be applied, see {@link HtmlEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml(final String text, final Writer writer, final HtmlEscapeType type, final HtmlEscapeLevel level)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        HtmlEscapeUtil.escape(new HtmlEscape.InternalStringReader(text), writer, type, level);

    }

    /**
     * <p>
     * Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml5(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml4Xml(Reader, Writer)} because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     * preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml5Xml(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     *   preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml4(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>Reader</tt> input,
     * writing results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as {@link #escapeHtml5Xml(Reader, Writer)} because
     * it will escape the apostrophe as <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for
     * such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(Reader, Writer, HtmlEscapeType, HtmlEscapeLevel)} with the following
     * preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml4Xml(final Reader reader, final Writer writer)
            throws IOException {
        escapeHtml(reader, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>Reader</tt> input, writing
     * results to a <tt>Writer</tt>.
     * </p>
     * <p>
     * This method will perform an escape operation according to the specified
     * {@link HtmlEscapeType} and {@link HtmlEscapeLevel}
     * argument values.
     * </p>
     * <p>
     * All other <tt>Reader</tt>/<tt>Writer</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @param type   the type of escape operation to be performed, see {@link HtmlEscapeType}.
     * @param level  the escape level to be applied, see {@link HtmlEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void escapeHtml(final Reader reader, final Writer writer, final HtmlEscapeType type, final HtmlEscapeLevel level)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        HtmlEscapeUtil.escape(reader, writer, type, level);

    }

    /**
     * <p>
     * Perform an HTML5 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML5 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(char[], int, int, java.io.Writer, HtmlEscapeType, HtmlEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len    the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml5(final char[] text, final int offset, final int len, final Writer writer)
            throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML5 level 1 (XML-style) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as
     * {@link #escapeHtml4Xml(char[], int, int, java.io.Writer)} because
     * it will escape the apostrophe as <tt>&amp;apos;</tt>, whereas in HTML 4 such NCR does not exist
     * (the decimal numeric reference <tt>&amp;#39;</tt> is used instead).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(char[], int, int, java.io.Writer, HtmlEscapeType, HtmlEscapeLevel)}
     * with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len    the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml5Xml(final char[] text, final int offset, final int len, final Writer writer)
            throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 2 (result is ASCII) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     * <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The five markup-significant characters: <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>,
     *       <tt>&quot;</tt> and <tt>&#39;</tt></li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by replacing those chars by the corresponding HTML 4 Named Character References
     *   (e.g. <tt>'&amp;acute;'</tt>) when such NCR exists for the replaced character, and replacing by a decimal
     *   character reference (e.g. <tt>'&amp;#8345;'</tt>) when there there is no NCR for the replaced character.
     * </p>
     * <p>
     *   This method calls {@link #escapeHtml(char[], int, int, java.io.Writer, HtmlEscapeType, HtmlEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len    the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml4(final char[] text, final int offset, final int len, final Writer writer)
            throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform an HTML 4 level 1 (XML-style) <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     * <em>Level 1</em> means this method will only escape the five markup-significant characters:
     * <tt>&lt;</tt>, <tt>&gt;</tt>, <tt>&amp;</tt>, <tt>&quot;</tt> and <tt>&#39;</tt>. It is called
     * <em>XML-style</em> in order to link it with JSP's <tt>escapeXml</tt> attribute in JSTL's
     * <tt>&lt;c:out ... /&gt;</tt> tags.
     * </p>
     * <p>
     * Note this method may <strong>not</strong> produce the same results as
     * {@link #escapeHtml5Xml(char[], int, int, java.io.Writer)}  because it will escape the apostrophe as
     * <tt>&amp;#39;</tt>, whereas in HTML5 there is a specific NCR for such character (<tt>&amp;apos;</tt>).
     * </p>
     * <p>
     * This method calls {@link #escapeHtml(char[], int, int, java.io.Writer, HtmlEscapeType, HtmlEscapeLevel)}
     * with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>type</tt>:
     *       {@link HtmlEscapeType#HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL}</li>
     *   <li><tt>level</tt>:
     *       {@link HtmlEscapeLevel#LEVEL_1_ONLY_MARKUP_SIGNIFICANT}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len    the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml4Xml(final char[] text, final int offset, final int len, final Writer writer)
            throws IOException {
        escapeHtml(text, offset, len, writer, HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
    }

    /**
     * <p>
     * Perform a (configurable) HTML <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     * This method will perform an escape operation according to the specified
     * {@link HtmlEscapeType} and {@link HtmlEscapeLevel}
     * argument values.
     * </p>
     * <p>
     * All other <tt>char[]</tt>-based <tt>escapeHtml*(...)</tt> methods call this one with preconfigured
     * <tt>type</tt> and <tt>level</tt> values.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len    the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @param type   the type of escape operation to be performed, see {@link HtmlEscapeType}.
     * @param level  the escape level to be applied, see {@link HtmlEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeHtml(final char[] text, final int offset, final int len, final Writer writer,
                                  final HtmlEscapeType type, final HtmlEscapeLevel level)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("The 'type' argument cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        final int textLen = (text == null ? 0 : text.length);

        if (offset < 0 || offset > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        if (len < 0 || (offset + len) > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        HtmlEscapeUtil.escape(text, offset, len, writer, type, level);

    }

    /**
     * <p>
     * Perform an HTML <strong>unescape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     * No additional configuration arguments are required. Unescape operations
     * will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be unescaped.
     * @return The unescaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     * same object as the <tt>text</tt> input argument if no unescaping modifications were required (and
     * no additional <tt>String</tt> objects will be created during processing). Will
     * return <tt>null</tt> if input is <tt>null</tt>.
     */
    public static String unescapeHtml(final String text) {
        if (text == null) {
            return null;
        }
        if (text.indexOf('&') < 0) {
            // Fail fast, avoid more complex (and less JIT-table) method to execute if not needed
            return text;
        }
        return HtmlEscapeUtil.unescape(text);
    }

    /**
     * <p>
     * Perform an HTML <strong>unescape</strong> operation on a <tt>String</tt> input, writing results to
     * a <tt>Writer</tt>.
     * </p>
     * <p>
     * No additional configuration arguments are required. Unescape operations
     * will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>String</tt> to be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void unescapeHtml(final String text, final Writer writer)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }
        if (text == null) {
            return;
        }
        if (text.indexOf('&') < 0) {
            // Fail fast, avoid more complex (and less JIT-table) method to execute if not needed
            writer.write(text);
            return;
        }

        HtmlEscapeUtil.unescape(new HtmlEscape.InternalStringReader(text), writer);

    }

    /**
     * <p>
     * Perform an HTML <strong>unescape</strong> operation on a <tt>Reader</tt> input, writing results to
     * a <tt>Writer</tt>.
     * </p>
     * <p>
     * No additional configuration arguments are required. Unescape operations
     * will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param reader the <tt>Reader</tt> reading the text to be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     * @since 1.1.2
     */
    public static void unescapeHtml(final Reader reader, final Writer writer)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        HtmlEscapeUtil.unescape(reader, writer);

    }

    /**
     * <p>
     * Perform an HTML <strong>unescape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     * No additional configuration arguments are required. Unescape operations
     * will always perform <em>complete</em> unescape of NCRs (whole HTML5 set supported), decimal
     * and hexadecimal references.
     * </p>
     * <p>
     * This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text   the <tt>char[]</tt> to be unescaped.
     * @param offset the position in <tt>text</tt> at which the unescape operation should start.
     * @param len    the number of characters in <tt>text</tt> that should be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if input is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void unescapeHtml(final char[] text, final int offset, final int len, final Writer writer)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        final int textLen = (text == null ? 0 : text.length);

        if (offset < 0 || offset > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        if (len < 0 || (offset + len) > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        HtmlEscapeUtil.unescape(text, offset, len, writer);

    }

    /*
     * This is basically a very simplified, thread-unsafe version of StringReader that should
     * perform better than the original StringReader by removing all synchronization structures.
     *
     * Note the only implemented methods are those that we know are really used from within the
     * stream-based escape/unescape operations.
     */
    private static final class InternalStringReader extends Reader {

        private String str;
        private final int length;
        private int next = 0;

        public InternalStringReader(final String s) {
            super();
            this.str = s;
            this.length = s.length();
        }

        @Override
        public int read() throws IOException {
            if (this.next >= length) {
                return -1;
            }
            return this.str.charAt(this.next++);
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                    ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (this.next >= this.length) {
                return -1;
            }
            int n = Math.min(this.length - this.next, len);
            this.str.getChars(this.next, this.next + n, cbuf, off);
            this.next += n;
            return n;
        }

        @Override
        public void close() throws IOException {
            this.str = null; // Just set the reference to null, help the GC
        }

    }


}