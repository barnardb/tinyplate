#!/bin/sh
set -euo pipefail

#[ -n "${GPG_TTY-}" ] || {
#  echo 'This should be run as `GPG_TTY=$(tty) ./release.sh`'
#  exit 2
#}

source ~/.bashrc.d/op.sh
mill all clean __.test __.mdoc __.updateReadme
PATH="$PWD:$PATH" mill mill.scalalib.PublishModule/publishAll \
  __.publishArtifacts \
  --sonatypeCreds "barnardb:$(opp sonatype)" \
  --release false \
  --gpgArgs "--local-user=11E95E562B2FE6597EEF0A45DAA3C69A20562CB2,--passphrase=$(opp gpg2020),--batch,--yes,-a,-b,--verify-options,no-show-photos,--pinentry-mode,loopback" \
  ;
