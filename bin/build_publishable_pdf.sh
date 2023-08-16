#!/bin/sh -x

me="$(readlink -f -- "$0")"
here="$(dirname -- "$me")"

# requires:
# xelatex
# convert (ImageMagick)
# qpdf

# if [ -z "$3" ] ; then
#     echo "usage: $0 NAME.tex user-pass owner-pass"
#     exit 1
# fi

cvt_opts="-alpha flatten -density 300 -compress jpeg -quality 35 -sampling-factor 4:2:0 -type TrueColor -interlace plane -define jpeg:dct-method=float -strip"
# encrypt_opts="--verbose --replace-input --linearize   --encrypt $2 $3 256 --extract=n --assemble=n --annotate=n --form=n --modify-other=n --print=none --cleartext-metadata --"
# show_opts="--password=$2 --check --show-object=trailer --show-pages --with-images"

if [ ! -e "$1" ] ; then
    echo "File not found: $1"
    exit 1
fi

dir="$(dirname -- "$1")"
dir="$(cd "$dir" >/dev/null 2>&1; pwd -P)"
base="$(basename -- "$1")"
fullname="$dir/$base"
nam="${base%.*}"
ext="${base##*.}"


echo "full: $fullname"
echo " dir: $dir"
echo "base: $base"
echo "name: $nam"
echo " ext: $ext"

t=$(mktemp -d)
cd $t || exit 1




# if [ $ext = lyx ] ; then
#     # convert lyx to pdf
#     lyx -batch -E pdf4 $nam-txt.pdf $nam.lyx
# elif [ $ext = context -o $ext = tex ] ; then
#    context --debug --result=$nam-txt.pdf $1
# elif [ $ext = fodt -o $ext = odt ] ; then
#     # export original ODT source document to (text-based) PDF file
#     soffice --headless --convert-to pdf $nam.fodt
#     mv $nam.pdf $nam-txt.pdf
# else
#     echo "filetype not supported"
#     exit 1
# fi
mkdir build
cd build || exit 1
xelatex -interaction=nonstopmode --shell-escape "$fullname" >$nam.pass1.log 2>&1
xelatex -interaction=nonstopmode --shell-escape "$fullname" >$nam.pass2.log 2>&1
mv -nv $nam.pdf $nam-txt.pdf
cd ..

mkdir $nam
cd $nam || exit 1
convert $cvt_opts ../build/$nam-txt.pdf $nam-%03d.jpg
#############exiftool -tagsfromfile ../../$nam.xmp -xmp:all -overwrite_original_in_place *.jpg
cd ..

cp -v $dir/$nam.xmp ./

$here/../SecurePdf-1.0.0/bin/SecurePdf \
    $nam \
    --keystore=/srv/arc/mosher.mine.nu.p12 \
    --graphic=/srv/arc/sigstamp.png \
    --page=2 \
    --height=36

sha384sum -b $nam.pdf >$nam.sha384
cat $nam.sha384
hash_value=$(cat $nam.sha384 | cut -d\  -f1 | xxd -r -p - | base32 -w 0 -)
echo "urn:hash:application/pdf:sha384:$hash_value" >$nam.urn

pwd




# build PDF with JPEG pages
# convert $cvt_opts $nam-txt.pdf $nam-jpg.pdf

# encrypt PDF files
# qpdf $encrypt_opts $nam-txt.pdf
# qpdf $encrypt_opts $nam-jpg.pdf

# copy metadata to jpg file
# exiftool -password "$2" -TagsFromFile $nam-txt.pdf '-xmp:all<all' $nam-jpg.pdf

# show PDF stats
# qpdf $show_opts $nam-txt.pdf
# pdfinfo -upw "$2" $nam-txt.pdf
# exiftool -password "$2" -a -G1 -s $nam-txt.pdf

# qpdf $show_opts $nam-jpg.pdf
# pdfinfo -upw "$2" $nam-jpg.pdf
# exiftool -password "$2" -a -G1 -s $nam-jpg.pdf
# exiftool -password "$2" -xmp -b $nam-jpg.pdf >$nam.xmp.xml

# ls -lht --full-time











# qpdf1="qpdf --verbose --encrypt foo bar 256 --extract=n --assemble=n --annotate=n --form=n --modify-other=n --print=none --"
# qpdf2="qpdf --verbose --password=foo --check --show-object=trailer --show-pages --with-images"
# cvjpg="convert -strip -interlace Plane -quality 33% -define jpeg:dct-method=float -sampling-factor 4:2:0"

# paper=$(grep \\papersize $1 | cut -d' ' -f 2 | xargs printf '--%s')

# # build "normal" PDF (text pages)
# $qpdf1 $nam-orig.pdf $nam-txt.pdf


# #-r 300 -scale-to 1040
# # convert text-based PDF to series of page images (PNG)
# pdftoppm -png -r 300 -forcenum $nam-orig.pdf $nam-page
# # assemble PNG images into PDF file
# pdfjam --noautoscale true $paper $nam-page-*.png -o $nam-png-raw.pdf
# # encrypt PDF file, for final output PDF(PNG)
# $qpdf1 $nam-png-raw.pdf $nam-png.pdf



# # build lossy JPEG images (from PNG images)
# for i in $nam-page-*.png ; do $cvjpg $i ${i%.*}.jpg ; done
# # assemble JPEG images into PDF file
# pdfjam --noautoscale true $paper $nam-page-*.jpg -o $nam-jpg-raw.pdf
# # encrypt PDF file, for final output PDF(JPG)
# $qpdf1 $nam-jpg-raw.pdf $nam-jpg.pdf



# #show PDF stats
# $qpdf2 $nam-txt.pdf
# $qpdf2 $nam-png.pdf
# $qpdf2 $nam-jpg.pdf



# NOTE: use i7j-rups to view internal structure of PDF files:
# java -jar /usr/local/lib/java/itext-rups.jar
