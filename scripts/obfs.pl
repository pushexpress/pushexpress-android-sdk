#!/usr/bin/env perl

use strict;
use warnings;

my $BASE_DIR = "sdkpushexpress/src/main";

chomp ( my @WORDS  = <DATA> );

my @MANIFEST_CLS = (
    "com.pushexpress.sdk.app_startup.SdkInitializer",
    "com.pushexpress.sdk.fcm.FcmService",
    "com.pushexpress.sdk.notification_actions.NotificationClickBroadcastReceiver",
    "com.pushexpress.sdk.notification_actions.TrampolineActivity",
);

my $INTENT_ACTION_CLICK = "com.pushexpress.sdk.ACTION_CLICK";

my @STRING_CONSTS = (
    q(NOTIFICATION_CHANNEL_ID = "sdkpushexpress_notification_channel"),
    q(SDK_TAG = "SdkPushExpress"),
    q(STORAGE_NAME = "sdkpushexpress"),
);

sub rename_click_intent {
    my $new_cname = join ".", map { $WORDS[int rand @WORDS] } (1 .. (2 + int(rand 2)));
    my $new_iname = join "_", map { uc } map { $WORDS[int rand @WORDS] } (1 .. (1 + int(rand 2)));

    print "going to rename $INTENT_ACTION_CLICK to $new_cname.$new_iname\n";
    my $cmd = qq(find $BASE_DIR -type f -print0 | xargs -0 perl -i -pe 's/\\b$INTENT_ACTION_CLICK\\b/$new_cname.$new_iname/g');
    run_cmd($cmd);
}

sub rename_const {
    my ($const) = @_;
    my ($cname, $cval) = ($const =~ /(\w+)\s*=\s*"([\w-]+)"/);

    my $new_cval = join "_", map { $WORDS[int rand @WORDS] } (1 .. (2 + int(rand 2)));
    $new_cval = substr($new_cval, 0, 22) if ($cname eq "SDK_TAG");

    print "going to rename $cname = $cval to $new_cval\n";
    my $cmd = qq(find $BASE_DIR -type f -print0 | xargs -0 perl -i -pe 's/\\b$cname = "$cval"\\b/$cname = "$new_cval"/g');
    run_cmd($cmd);
}

sub rename_constants {
   map { rename_const($_) } @STRING_CONSTS;
}

sub rename_cls {
    my ($cls) = @_;

    my @parts = split /\./, $cls;
    my $path = join "/", ($BASE_DIR, "java", @parts[0 .. ($#parts - 1)]);

    my $cname = $parts[$#parts];

    my $new_cname = join "", map { ucfirst } map { $WORDS[int rand @WORDS] } (1 .. (2 + int(rand 2)));

    print "going to rename class $cname to $new_cname in $path ...\n";

    # rename class occurrences
    my $cmd = qq(find $BASE_DIR -type f -print0 | xargs -0 perl -i -pe 's/\\b$cname\\b/$new_cname/g');
    run_cmd($cmd);

    # rename class file
    run_cmd("mv $path/$cname.kt $path/$new_cname.kt");
}

sub rename_pkg {
    my ($pkg) = @_;

    my @parts = split /\./, $pkg;
    my $path = join "/", ($BASE_DIR, "java", @parts);

    my $pname = $parts[$#parts];

    my $new_pname = join "_", map { $WORDS[int rand @WORDS] } (1 .. (1 + int(rand 2)));
    my $new_path = join "/", ($BASE_DIR, "java", @parts[0 .. ($#parts - 1)], $new_pname);
    my $new_pkg = join ".", @parts[0 .. ($#parts - 1)], $new_pname;

    print "going to rename package $pkg to $new_pkg ...\n";

    # rename package occurrences
    my $cmd = qq(find $BASE_DIR -type f -print0 | xargs -0 perl -i -pe 's/\\b$pkg\\b/$new_pkg/g');
    run_cmd($cmd);

    # rename pkg path
    rename $path, $new_path;
}

sub packages_from_classes {
    my (@clss) = @_;
    my %pkgs;
    for my $cls (@clss) {
        my @parts = split /\./, $cls;
        my $pkg = join ".", @parts[0 .. ($#parts - 1)];
        $pkgs{$pkg} = 1;
    }
    return keys %pkgs;
}

sub run_cmd {
    my ($cmd) = @_;
    print "run: $cmd\n";
    # my $ret = system($cmd);
    # $ret >>= 8;
    # die "FAILED with code $ret\n" if $ret > 0;

    # return $ret > 0 ? 0 : 1;
}

sub main {
    for my $cls (@MANIFEST_CLS) {
        rename_cls($cls);
    }

    for my $pkg (packages_from_classes(@MANIFEST_CLS)) {
        rename_pkg($pkg);
    }

    rename_click_intent();
    rename_constants();
}

main();

__DATA__
analogist
admitter
antipleion
andrographolide
alveoli
antrophore
angulous
argentiferous
adoptionist
aphylly
branchway
bangiaceous
bebrine
ballium
bronchiolitis
baldcrown
bedral
benzonaphthol
bridewain
browed
cornuate
camphorphorone
challenge
cardioblast
crunodal
countercommand
champerty
citatory
chloromethane
cryptoheresy
dictyoceratine
dicotyledonary
directress
disconcerting
devolutionary
disentomb
doughlike
dimagnesic
discalceate
disnaturalize
epitaph
encake
emaciate
epistolatory
ensiform
exceptionable
euonymous
esthesiogenic
epicurish
exopterygotous
farmtown
fierily
fearnought
fencible
forgivableness
forcedness
foothook
falces
felliducous
filings
gangling
gypsiferous
gausterer
gritrock
genteelize
gentlemanly
gregale
golfdom
granitical
gelatinizable
handicraftsmanship
heterozygotic
hornblendic
harvestless
hygrophyte
hippurite
housewifeliness
hindhead
hagbush
harquebusade
implacental
inoperative
infracelestial
incantation
interinhibitive
intestation
irrelate
isocholesterol
infrastipular
interaccessory
jetware
jayhawk
jostler
jararacussu
juncaceous
jockeyism
joyously
judicator
jennerize
jettyhead
kenmark
kickshaw
kirkify
kasher
kymographic
keratoplastic
kavaic
kavass
karyomiton
kartel
lengthful
lamellarly
luciferoid
lithophotogravure
leadage
laciniform
lingberry
limnobiology
litterateur
latite
macaroon
microcosmian
microorganic
miscall
mumblebee
microblephary
muffish
mobster
maffia
medicable
nonvitreous
nonstriker
nematelminth
newmarket
nonconnective
nonparishioner
nervosism
noneducational
nectar
nodiak
osteoclast
overlisten
outlaid
oenopoetic
obtuseness
oligodynamic
overgirdle
ouachitite
overhastily
overwiped
plunderbund
preadministrator
promodernist
pulsatile
patriot
prefiction
pernicious
peoplet
precounsellor
paleoichthyology
quatrocento
questionary
quotation
quadribasic
queencraft
quadrennially
quadrangulate
quirquincho
questionably
quinyl
reregister
reharmonize
rupturewort
revenger
rattlebox
revisible
recruit
rhebok
remend
replate
satinlike
subsistence
stiltlike
semiperimeter
seventhly
seismographer
septave
superhirudine
starchwort
staphyloedema
tussah
tartwoman
tailhead
thievable
theophilanthropy
tripleback
toweling
tetrapterous
tabourer
technicalize
unripely
unmonistic
unquaking
unrhyme
underpeopled
untranslatableness
untragic
uncentred
uterine
untwinable
vesication
virtualism
variolite
villanous
vertebra
vicarage
valerianaceous
vulturelike
vaporarium
vetchy
withstand
whitter
woundability
woodgrub
wearable
withery
wardholding
wirebar
widespreadly
waxworker
xiphuous
xerophytically
xarque
xenophobian
xiphisterna
xylophage
xenacanthine
xanthydrol
xenocryst
xantholeucophore
yander
yearful
youthy
yallow
youngling
yodelist
yellowcrown
yuckel
youthtide
yeldrock
zoiatria
zootic
zabtie
zirconifluoride
zealotism
zealously
zoophilist
zincate
zorillo
zymophore
