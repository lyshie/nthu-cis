#!/usr/bin/perl -w

#===============================================================================
#
#         FILE: captcha_decoder.pl
#
#        USAGE: ./captcha_decoder.pl [IMAGE-FILE]
#
#  DESCRIPTION: Captcha Decoder for NTHU AIS
#
#      OPTIONS: ---
# REQUIREMENTS: ---
#         BUGS: ---
#        NOTES: ---
#       AUTHOR: SHIE, Li-Yi <lyshie@mx.nthu.edu.tw>
# ORGANIZATION:
#      VERSION: 1.0
#      CREATED: 2013/02/27 15:20:00
#     REVISION: ---
#===============================================================================

use strict;
use warnings;

use FindBin qw($Bin);
use GD;
use Image::OCR::Tesseract qw(get_ocr);
use File::Basename;

my $FILENAME = $ARGV[0];
my $FS       = basename($FILENAME);

open( PNG, $FILENAME );

my $im    = GD::Image->newFromPng( \*PNG );
my $total = $im->colorsTotal();
my $white = $im->colorAllocate( 255, 255, 255 );
my $black = $im->colorAllocate( 0, 0, 0 );

for my $x ( 0 .. $im->width() - 1 ) {
    for my $y ( 0 .. $im->height() - 1 ) {
        my $index = $im->getPixel( $x, $y );
        my ( $r, $g, $b ) = $im->rgb($index);
        if ( $r < 160 || $g < 160 || $b < 160 ) {
            $im->setPixel( $x, $y, $black );
        }
        else {
            $im->setPixel( $x, $y, $white );

        }
    }
}

open( OUTPUT, ">/tmp/$FS" );
binmode(OUTPUT);
print OUTPUT $im->png();
close(OUTPUT);

close(PNG);

# lyshie_20120409: guest and correct
my $string = get_ocr("/tmp/$FS");

$string =~ s/[Oo°]+/0/g;
$string =~ s/[\\I‘]+/1/g;
$string =~ s/[Z?]+/2/g;
$string =~ s/[A]+/4/g;
$string =~ s/[e]+/6/g;
$string =~ s/[SB]+/8/g;
$string =~ s/[g]+/9/g;
$string =~ s/[\s\-]//g;

print "$string\n";
