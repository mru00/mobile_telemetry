# Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

include Makefile.settings




.PHONY: all
all: upload


$(build):
	$(MAKE) -f Makefile.compile GNU_INSTALL_ROOT=$(GNU_INSTALL_ROOT)

$(target): $(build)
	srec_cat $(softdevice) -intel $< -intel -o $@ -intel --line-length=44

.PHONY: build
build: $(target)

.PHONY: upload
upload: $(target)
	cp $(target) /media/mru/MBED

.PHONY: clean
clean:
	$(RM) $(target)
	$(MAKE) -f Makefile.compile clean
