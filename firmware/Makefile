
include Makefile.settings




.PHONY: all
all: upload


$(build):
	$(MAKE) -f Makefile.compile

$(target): $(build)
	srec_cat $(softdevice) -intel $(build) -intel -o $@ -intel --line-length=44


.PHONY: upload
upload: $(target)
	cp $(target) /media/mru/MBED

.PHONY: clean
clean:
	$(MAKE) -f Makefile.compile clean