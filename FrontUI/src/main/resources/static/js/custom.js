$(function () {

    // Header Scroll
    $(window).scroll(function () {
        if ($(window).scrollTop() >= 60) {
            $("header").addClass("fixed-header").hide().fadeIn(300);
        } else {
            $("header").removeClass("fixed-header");
        }
    });


    // Featured Owl Carousel
    $('.featured-projects-slider .owl-carousel').owlCarousel({
        center: true,
        loop: true,
        margin: 30,
        nav: true, // Enabled navigation buttons
        navText: ["<span class='nav-btn prev-slide'>&#10094;</span>", "<span class='nav-btn next-slide'>&#10095;</span>"],
        dots: true, // Enabled dots for better navigation
        autoplay: true,
        autoplayTimeout: 5000,
        autoplayHoverPause: true, // Pause on hover for better UX
        responsive: {
            0: {
                items: 1
            },
            600: {
                items: 2
            },
            1000: {
                items: 3
            },
            1200: {
                items: 4
            }
        }
    })


    // Count
    $('.count').each(function () {
		$(this).prop('Counter', 0).animate({
			Counter: $(this).text()
		}, {
			duration: 1500, // Increased duration for smoother animation
			easing: 'linear', // Changed easing for consistent speed
			step: function (now) {
				$(this).text(Math.ceil(now));
			}
		});
	});


    // ScrollToTop
    function scrollToTop() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    }

    const btn = document.getElementById("scrollToTopBtn");
    btn.addEventListener("click", scrollToTop);

    window.onscroll = function () {
        const btn = document.getElementById("scrollToTopBtn");
        if (document.documentElement.scrollTop > 100 || document.body.scrollTop > 100) {
            btn.style.display = "flex";
        } else {
            btn.style.display = "none";
        }
    };


    // Aos
	AOS.init({
		once: true,
	});


    function removeCookie(name) {
      document.cookie = name + '=; Max-Age=0; path=/; domain=localhost; Secure; SameSite=Strict';
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
      logoutBtn.onclick = function() {
        fetch('http://localhost:13000/user/v1/logout', { method: 'POST', credentials: 'include' })
          .finally(() => {
            removeCookie('jwtAccessToken');
            removeCookie('jwtRefreshToken');
            window.location.href = 'sign-in.html';
          });
      };
    }

});
