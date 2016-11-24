#!/bin/sh

TMP=$(wget --quiet -A '*auth_img*' -r --level=1 -P /tmp/auth_img/ "https://www.ccxp.nthu.edu.tw/ccxp/INQUIRE/index.php?lang=chinese" -e robots=off)

AUTH_IMG=$(ls -t -r /tmp/auth_img/www.ccxp.nthu.edu.tw/ccxp/INQUIRE/*auth_img* | tail -n 1)

PLAIN_TEXT=$(/usr/share/lyshie-tools/scripts/captcha_decoder.pl "$AUTH_IMG");

yad --image="$AUTH_IMG" --entry --entry-label="我猜應該是" --entry-text="$PLAIN_TEXT"

basename "$AUTH_IMG"
